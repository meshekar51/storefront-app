#!/usr/bin/env bash
# Install the four cluster add-ons with Helm (idempotent — safe to re-run).
#
#   metrics-server               → CPU/memory metrics for the HPAs
#   external-secrets             → AWS Secrets Manager → Kubernetes Secrets
#   aws-load-balancer-controller → fills the ALB target group with Envoy pod IPs
#   envoy-gateway                → Gateway API implementation (+ Gateway API CRDs)
set -euo pipefail
source "$(dirname "$0")/00-env.sh"
ADDONS="$EKS_DIR/addons"

helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server >/dev/null
helm repo add external-secrets https://charts.external-secrets.io >/dev/null
helm repo add eks https://aws.github.io/eks-charts >/dev/null
helm repo update >/dev/null

echo "▶ metrics-server"
helm upgrade --install metrics-server metrics-server/metrics-server \
  --namespace kube-system --version "$METRICS_SERVER_CHART_VERSION" \
  -f "$ADDONS/metrics-server.yaml" --wait

echo "▶ external-secrets"
helm upgrade --install external-secrets external-secrets/external-secrets \
  --namespace external-secrets --create-namespace --version "$EXTERNAL_SECRETS_CHART_VERSION" \
  -f "$ADDONS/external-secrets.yaml" --wait

echo "▶ aws-load-balancer-controller"
render < "$ADDONS/aws-load-balancer-controller.yaml" > /tmp/lbc-values.yaml
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --namespace kube-system --version "$AWS_LBC_CHART_VERSION" \
  -f /tmp/lbc-values.yaml --wait

echo "▶ envoy-gateway"
helm upgrade --install envoy-gateway oci://docker.io/envoyproxy/gateway-helm \
  --namespace envoy-gateway-system --create-namespace --version "$ENVOY_GATEWAY_VERSION" \
  -f "$ADDONS/envoy-gateway.yaml" --wait
kubectl wait --for=condition=Available deployment/envoy-gateway -n envoy-gateway-system --timeout=5m

# The load balancer controller adds a readiness gate to Envoy pods in this
# namespace: a new Envoy pod counts as Ready only once the ALB sees it healthy,
# so rolling updates of the gateway never drop traffic.
kubectl label namespace envoy-gateway-system elbv2.k8s.aws/pod-readiness-gate-inject=enabled --overwrite

echo
echo "✅ Add-ons installed. Next: ./scripts/03-deploy-platform.sh"
