#!/usr/bin/env bash
# Namespace, Gateway, ALB target-group binding and secrets.
set -euo pipefail
source "$(dirname "$0")/00-env.sh"

kubectl kustomize "$EKS_DIR/platform" | render | kubectl apply -f -

echo "▶ waiting for secrets to sync from AWS Secrets Manager"
kubectl wait externalsecret --all -n storefront --for=condition=Ready --timeout=2m

echo "▶ waiting for the Gateway to be programmed (Envoy pods up)"
kubectl wait gateway/storefront -n storefront --for=condition=Programmed --timeout=5m

echo "▶ target group binding"
kubectl get targetgroupbinding -n envoy-gateway-system

echo
echo "✅ Platform ready. Next: ./scripts/04-deploy-apps.sh <image-tag>"
