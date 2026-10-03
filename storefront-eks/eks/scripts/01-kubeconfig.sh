#!/usr/bin/env bash
# Point kubectl at the new cluster.
set -euo pipefail
source "$(dirname "$0")/00-env.sh"

aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME" --alias "$CLUSTER_NAME"
kubectl get nodes -o wide
