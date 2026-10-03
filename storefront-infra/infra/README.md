# Storefront — AWS infrastructure (Terraform)

Everything AWS-side, in one plain Terraform root. No custom modules, ten short files.

```
infra/
├── versions.tf             providers + S3 remote state
├── variables.tf            the knobs (region, domain, sizes, high_availability)
├── vpc.tf                  VPC: public / private / database subnets in 3 AZs
├── eks.tf                  EKS cluster + managed node group + core add-ons
├── security-groups.tf      who may talk to what
├── data-stores.tf          RDS PostgreSQL · ElastiCache Redis · Amazon MQ (RabbitMQ 4.2)
├── secrets.tf              one Secrets Manager secret per data store
├── edge.tf                 Route 53 · ACM certificate · ALB · WAF
├── registry-and-iam.tf     7 ECR repos · Pod Identity roles for 2 controllers
├── outputs.tf              values the eks/ scripts read
└── terraform.tfvars.example
```

## What gets created

| Layer | Resource | Why |
|---|---|---|
| DNS | Route 53 `A` alias `shop.<domain>` → ALB | users type a name, not an ALB hostname |
| TLS | ACM certificate, DNS-validated | HTTPS terminates at the ALB |
| Edge | ALB (public subnets), HTTP → HTTPS redirect | single entry point |
| Edge | WAF: rate limit 2,000 req / 5 min / IP + 4 AWS managed rule groups | blocks bots, SQLi, known exploits before they reach the cluster |
| Edge | Target group (type `ip`, port 10080) | filled with Envoy Gateway pod IPs by Kubernetes |
| Compute | EKS 1.35, 3 × t3.large nodes in private subnets | runs gateway + services |
| Data | RDS PostgreSQL 17, encrypted, private | one DB, one schema per service |
| Data | ElastiCache Redis 7.1, TLS + AUTH token | catalogue cache |
| Messaging | Amazon MQ for RabbitMQ 4.2, private, AMQPS | order events |
| Secrets | `storefront/postgres`, `storefront/redis`, `storefront/rabbitmq` | endpoint + password per store, read by External Secrets |
| Registry | `storefront/<service>` ECR repos, immutable tags, scan on push | images |
| IAM | Pod Identity roles for AWS Load Balancer Controller and External Secrets | no static AWS keys in the cluster |

### Network rules

```
internet ──443──▶ ALB SG ──10080──▶ node SG (Envoy pods)
                                    node SG ──5432──▶ RDS SG
                                    node SG ──6379──▶ Redis SG
                                    node SG ──5671──▶ MQ SG
```
Data stores sit in the **database subnets**, which have no route to the internet.

## Apply

```bash
cp terraform.tfvars.example terraform.tfvars     # set domain_name to a hosted zone you own
terraform init -backend-config="bucket=<your-state-bucket>"
terraform plan -out tfplan
terraform apply tfplan                           # ~25 min (EKS + MQ are the slow ones)
```

Then configure kubectl:

```bash
aws eks update-kubeconfig --region eu-central-1 --name storefront
```

…and continue in the **eks/** folder.

## Cost (eu-central-1, approx., `high_availability = false`)

| Item | ≈ / month |
|---|---|
| EKS control plane | $73 |
| 3 × t3.large nodes | $180 |
| NAT gateway (1) | $37 + data |
| ALB + WAF | $25 + $10 + requests |
| RDS db.t4g.small | $30 |
| ElastiCache cache.t4g.micro | $15 |
| Amazon MQ mq.m7g.medium | ~$60 |
| **Total** | **≈ $430** |

`high_availability = true` → Multi-AZ RDS, Redis replica, 3-node MQ cluster, NAT per AZ: roughly 2–3×.

## Destroy

Delete the Kubernetes apps first (so the load balancer controller deregisters targets), then:

```bash
terraform destroy
```

## Production upgrades (kept out on purpose)

- One database user per service with rights to its own schema only (today: one shared user).
- RDS-managed master password + Secrets Manager rotation.
- `deletion_protection = true`, `high_availability = true`.
- Private EKS API endpoint + VPN / SSM access.
