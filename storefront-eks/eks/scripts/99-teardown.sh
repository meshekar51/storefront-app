#!/usr/bin/env bash
# Remove everything from the cluster, in the right order, BEFORE `terraform destroy`.
# (The TargetGroupBinding must go while the controller is still running, so it
#  deregisters the pod IPs from the ALB target group cleanly.)
set -euo pipefail
source "$(dirname "$0")/00-env.sh"

kubectl delete -k "$EKS_DIR/apps" --ignore-not-found
kubectl delete targetgroupbinding storefront-gateway -n envoy-gateway-system --ignore-not-found --wait
kubectl delete gateway storefront -n storefront --ignore-not-found --wait
kubectl delete -k "$EKS_DIR/platform" --ignore-not-found

helm uninstall envoy-gateway -n envoy-gateway-system || true
helm uninstall aws-load-balancer-controller -n kube-system || true
helm uninstall external-secrets -n external-secrets || true
helm uninstall metrics-server -n kube-system || true

echo "✅ Cluster is empty. Now: cd ../infra && terraform destroy"
