#!/usr/bin/env bash
# Deploy (or upgrade) the storefront services.
#   ./scripts/04-deploy-apps.sh 3fa9c1d0be72     # the tag pushed by app/scripts/build-and-push.sh
set -euo pipefail
export IMAGE_TAG="${1:?usage: $0 <image-tag>}"
source "$(dirname "$0")/00-env.sh"

kubectl kustomize "$EKS_DIR/apps" | render | kubectl apply -f -

for d in catalogue cart inventory payment order notification frontend; do
  kubectl rollout status deployment/"$d" -n storefront --timeout=5m
done

echo
echo "✅ ${IMAGE_TAG} is live → ${APP_URL}"
echo "   Check: ./scripts/05-verify.sh"
