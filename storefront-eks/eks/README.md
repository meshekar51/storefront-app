# Storefront — EKS platform & deployment

Everything that runs **inside** the cluster: the four add-ons, the Gateway API
front door, the bridge to the Terraform-owned ALB, secrets, and the seven app
workloads. Plain YAML + Kustomize + a handful of numbered scripts.

```
eks/
├── versions.env                    pinned chart versions
├── addons/                         Helm values for the 4 add-ons
│   ├── aws-load-balancer-controller.yaml
│   ├── envoy-gateway.yaml
│   ├── external-secrets.yaml
│   └── metrics-server.yaml
├── platform/                       shared plumbing (kustomize)
│   ├── namespace.yaml              storefront ns, Pod Security "restricted"
│   ├── gateway.yaml                EnvoyProxy · GatewayClass · Gateway · ClientTrafficPolicy
│   ├── target-group-binding.yaml   ALB target group ⇄ Envoy pods
│   └── external-secrets.yaml       ClusterSecretStore + 3 ExternalSecrets
├── apps/                           the application (kustomize)
│   ├── config.yaml                 service URLs, log format
│   ├── catalogue.yaml … frontend.yaml   Deployment · Service · HPA · PDB per service
│   ├── routes.yaml                 HTTPRoutes (path → Service)
│   └── network-policies.yaml       who may call whom
└── scripts/
    ├── 00-env.sh                   reads ../infra Terraform outputs
    ├── 01-kubeconfig.sh
    ├── 02-install-addons.sh
    ├── 03-deploy-platform.sh
    ├── 04-deploy-apps.sh <tag>
    ├── 05-verify.sh                end-to-end test through the real URL
    └── 99-teardown.sh
```

---

## 1. How a request travels

```
 browser ──https://shop.example.com/api/products──────────────────────────────────────┐
                                                                                       │
 ① Route 53        A-alias shop.example.com → ALB                         (Terraform) │
 ② ALB + WAF       TLS ends here · WAF rules · forward to target group    (Terraform) │
 ③ Target group    IPs of the Envoy pods, port 10080         (filled by TargetGroupBinding)
 ④ Envoy Gateway   matches HTTPRoute "GET /api/products" → Service catalogue  (Gateway API)
 ⑤ Service         catalogue:80 → a ready pod :8080                        (kube-proxy)
 ⑥ Pod             Spring Boot → Redis (hit) or PostgreSQL (miss)
 ⑦ AWS data        ElastiCache (TLS) · RDS (TLS) · Amazon MQ (AMQPS) via security groups
```

**Why split it this way?** The ALB, WAF, certificate and DNS change rarely and need
AWS permissions — Terraform owns them. *Routing* changes every time a service is added —
Kubernetes owns it, through Gateway API objects that developers can read and review.
The **TargetGroupBinding** is the only link between the two worlds.

| Hop | Object | File |
|---|---|---|
| ALB → Envoy pods | `TargetGroupBinding storefront-gateway` | `platform/target-group-binding.yaml` |
| Envoy config | `EnvoyProxy storefront-proxy`, `GatewayClass envoy`, `Gateway storefront` | `platform/gateway.yaml` |
| path → service | `HTTPRoute` × 5 | `apps/routes.yaml` |
| service → pods | `Service` per app | `apps/<app>.yaml` |

### Routing table

| Method | Path | Service | Notes |
|---|---|---|---|
| GET | `/api/products/**` | catalogue | read-only, cached |
| any | `/api/carts/**` | cart | |
| any | `/api/orders/**` | order | 30 s timeout (calls 4 services) |
| GET | `/api/inventory/**` | inventory | POST reservations stay internal |
| any | `/` | frontend | React app + `/healthz` for the ALB |
| — | `/api/payments` | **not routed** | only order may call payment |

The most specific path wins, so `/api/...` never falls through to the frontend. If an
`/api/` path matches no route it reaches the frontend nginx, which answers `404`.

---

## 2. The add-ons and why each is there

| Add-on | Namespace | Job in this project | AWS access |
|---|---|---|---|
| **metrics-server** | kube-system | CPU metrics → HPAs scale catalogue, cart, order, frontend | none |
| **External Secrets Operator** | external-secrets | Secrets Manager `storefront/*` → Kubernetes Secrets | Pod Identity, read 3 secrets only |
| **AWS Load Balancer Controller** | kube-system | registers Envoy pod IPs in the ALB target group | Pod Identity |
| **Envoy Gateway** | envoy-gateway-system | Gateway API implementation; runs 2 Envoy pods | none |

Already installed by Terraform as EKS managed add-ons: VPC CNI (with **NetworkPolicy
enforcement on**), CoreDNS, kube-proxy, EKS Pod Identity Agent.

Pod Identity means: the controller's ServiceAccount is mapped to an IAM role by AWS
(`aws_eks_pod_identity_association`). No IAM keys, no IRSA annotations, nothing to rotate.

---

## 3. Secrets: from AWS to the pod

```
AWS Secrets Manager                 Kubernetes (namespace storefront)          Pod env
storefront/postgres ─┐               Secret postgres  host port dbname          DB_HOST, DB_PORT, DB_NAME,
                     │  External     username password                 ───▶     DB_USERNAME, DB_PASSWORD
storefront/redis ────┼─ Secrets ──▶ Secret redis     host port password ───▶     REDIS_HOST, REDIS_PASSWORD
storefront/rabbitmq ─┘  Operator    Secret rabbitmq  url username password ──▶   MQ_URL, MQ_USERNAME, MQ_PASSWORD
```

Each Deployment builds its JDBC URL from those values, choosing its own schema:

```yaml
- name: DB_URL
  value: jdbc:postgresql://$(DB_HOST):$(DB_PORT)/$(DB_NAME)?sslmode=require&currentSchema=catalogue
```

`sslmode=require` because RDS PostgreSQL enforces TLS. Redis uses `REDIS_SSL=true`
(ElastiCache in-transit encryption). The MQ URL is `amqps://…:5671`, which turns TLS on.

---

## 4. What every workload gets

Each `apps/<service>.yaml` holds the same pattern, so once you have read one you have read them all:

| Setting | Value | Why |
|---|---|---|
| replicas | 2 (HPA 2–6 for public services) | survive a pod or node loss |
| `topologySpreadConstraints` | spread across zones | survive an AZ loss |
| `strategy` | `maxSurge 1, maxUnavailable 0` | zero-downtime rollouts |
| `startupProbe` | `/actuator/health/liveness`, up to 3 min | JVM start + Flyway migrations |
| `readinessProbe` | `/actuator/health/readiness` | no traffic until ready |
| `preStop: sleep 10` | | Envoy stops routing before Spring stops |
| `terminationGracePeriodSeconds` | 45 | Spring's graceful shutdown finishes in-flight requests |
| resources | req 250m / 768Mi, limit 1Gi memory, **no CPU limit** | JVM heap = 75 % of the limit; no CPU throttling |
| security | non-root, read-only root FS, no capabilities, seccomp | passes Pod Security `restricted` |
| `automountServiceAccountToken: false` | | apps never call the Kubernetes API |
| PDB | `minAvailable: 1` | node upgrades drain one pod at a time |

### Network policies (zero trust inside the namespace)

```
envoy-gateway-system ──▶ frontend, catalogue, cart, order, inventory   :8080
cart, order          ──▶ catalogue                                     :8080
order                ──▶ cart, inventory, payment                      :8080
everything else      ──✗  (default-deny ingress)
```

Outbound traffic is open; the data stores are protected by AWS security groups instead.

---

## 5. Deploy, step by step

**Prerequisites:** `aws` CLI v2, `kubectl` ≥ 1.33, `helm` ≥ 3.14, `jq`, and a finished
`terraform apply` in the sibling `infra/` folder (or set the variables from
`scripts/00-env.sh` yourself).

```bash
cd eks

./scripts/01-kubeconfig.sh          # kubectl → the new cluster
./scripts/02-install-addons.sh      # 4 Helm releases, ~3 min
./scripts/03-deploy-platform.sh     # namespace, Gateway, ALB binding, secrets

# build + push images from the app/ folder:
#   (cd ../app && ./scripts/build-and-push.sh)   → prints the tag
./scripts/04-deploy-apps.sh 3fa9c1d0be72

./scripts/05-verify.sh              # real HTTPS calls: products, cart, checkout, payment hidden
```

`05-verify.sh` output when everything works:

```
▶ HTTP checks against https://shop.example.com
  ✅ frontend /
  ✅ catalogue: 8 products
  ✅ cart: item added
  ✅ order: CONFIRMED
  ✅ payment is not reachable from the internet (404)
▶ notification (last log lines — should show the email for the order above)
… 📧 Email to customer: order ORD-8F21C0D9A4 confirmed — 1 item(s), total 49.90 EUR
```

### Upgrade a release

```bash
(cd ../app && ./scripts/build-and-push.sh)      # new tag
./scripts/04-deploy-apps.sh <new-tag>           # rolling update, no downtime
```

### Roll back

```bash
kubectl rollout undo deployment/order -n storefront      # one service
./scripts/04-deploy-apps.sh <previous-tag>               # everything
```

---

## 6. Day-2 operations

| Task | Command |
|---|---|
| What is running | `kubectl get pods,svc,httproute,hpa -n storefront` |
| Logs of a service | `kubectl logs -n storefront deploy/order -f` |
| Emails being "sent" | `kubectl logs -n storefront deploy/notification -f` |
| Is the ALB happy | `aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN` |
| Gateway status | `kubectl get gateway storefront -n storefront -o yaml` (look for `Programmed: True`) |
| Route attached? | `kubectl describe httproute catalogue -n storefront` (look for `Accepted: True`) |
| Secrets synced? | `kubectl get externalsecret -n storefront` (`SecretSynced`) |
| Scale by hand | `kubectl scale deploy/catalogue -n storefront --replicas=4` (HPA takes over again) |
| Restart after a password rotation | `kubectl rollout restart deploy -n storefront` |
| Talk to a service directly | `kubectl port-forward -n storefront svc/payment 8084:80` → `curl localhost:8084/actuator/health` |

## 7. Troubleshooting

| Symptom | Likely cause | Check / fix |
|---|---|---|
| Browser: **502/503** from the ALB | no healthy targets | `describe-target-health`; Envoy pods running? `kubectl get pods -n envoy-gateway-system` |
| Targets stay `initial` / `unhealthy` | SG rule ALB → nodes :10080 missing, or `/healthz` route broken | `curl` the frontend pod `/healthz`; check `nodes_from_alb` rule in Terraform |
| TargetGroupBinding error | LBC lacks permissions / wrong ARN | `kubectl describe targetgroupbinding -n envoy-gateway-system`; LBC logs in kube-system |
| **404 from Envoy** for `/api/...` | HTTPRoute not attached | `kubectl describe httproute …` → `Accepted`, `ResolvedRefs` |
| Pod `CreateContainerConfigError` | Kubernetes Secret not there yet | `kubectl describe externalsecret postgres -n storefront` |
| ExternalSecret `SecretSyncedError` | ESO can't read AWS secret | Pod Identity association `external-secrets/external-secrets`; secret name `storefront/postgres` |
| Pod `CrashLoopBackOff`, log says `Connection refused` / timeout to RDS | SG or wrong host | node SG → RDS SG :5432 rule; `DB_HOST` value |
| `FATAL: no pg_hba.conf entry … no encryption` | missing TLS | keep `sslmode=require` in `DB_URL` |
| Catalogue: `RedisConnectionException` | TLS off | `REDIS_SSL=true` (ElastiCache has in-transit encryption) |
| Order: `AmqpConnectException` | MQ URL / SG :5671 | URL must start with `amqps://` |
| Calls between services time out | NetworkPolicy | add the arrow to `apps/network-policies.yaml` |
| Pods `Pending` | not enough nodes | `kubectl describe pod`; raise `node_desired_size` in Terraform |

## 8. GitOps (optional)

These folders work unchanged with Argo CD: point an Application at `eks/apps` and
replace the `__ECR_REGISTRY__ / __IMAGE_TAG__` placeholders with a Kustomize `images:`
block committed by your pipeline. The pipelines from the DevSecOps project
(`argocd-sync.yml`) do exactly that commit.

## 9. Tear down

```bash
./scripts/99-teardown.sh            # apps → binding → gateway → add-ons
cd ../infra && terraform destroy
```

Order matters: the TargetGroupBinding must be deleted while the load balancer
controller still runs, so it deregisters the targets before Terraform deletes the target group.
