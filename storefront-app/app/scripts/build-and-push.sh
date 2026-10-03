#!/usr/bin/env bash
# Build all 7 images and push them to ECR.
#
#   ./scripts/build-and-push.sh                 # tag = current git short SHA
#   ./scripts/build-and-push.sh v1.0.0          # explicit tag
#
# Needs: docker (buildx), aws CLI logged in, AWS_REGION set (default eu-central-1).
set -euo pipefail

cd "$(dirname "$0")/.."
REGION="${AWS_REGION:-eu-central-1}"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com"
TAG="${1:-$(git rev-parse --short=12 HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"

SERVICES=(catalogue-service cart-service inventory-service payment-service order-service notification-service)

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

for svc in "${SERVICES[@]}"; do
  echo "▶ $svc"
  docker buildx build --platform linux/amd64 --build-arg SERVICE="$svc" \
    -t "${REGISTRY}/storefront/${svc}:${TAG}" --push .
done

echo "▶ frontend"
docker buildx build --platform linux/amd64 -t "${REGISTRY}/storefront/frontend:${TAG}" --push ./frontend

echo
echo "Pushed tag ${TAG} to ${REGISTRY}/storefront/*"
echo "Next: in the eks folder run  ./scripts/04-deploy-apps.sh ${TAG}"
