#!/usr/bin/env bash
# Reads the Terraform outputs that the cluster setup needs.
#   source scripts/00-env.sh
# Expects the infra folder next to this one (../infra). Override with INFRA_DIR=/path/to/infra.
# Any variable already set in your shell wins (useful in CI without Terraform state access).

EKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="${INFRA_DIR:-$EKS_DIR/../infra}"

_tf() { terraform -chdir="$INFRA_DIR" output -raw "$1"; }

export AWS_REGION="${AWS_REGION:-$(_tf region)}"
export CLUSTER_NAME="${CLUSTER_NAME:-$(_tf cluster_name)}"
export VPC_ID="${VPC_ID:-$(_tf vpc_id)}"
export TARGET_GROUP_ARN="${TARGET_GROUP_ARN:-$(_tf gateway_target_group_arn)}"
export ECR_REGISTRY="${ECR_REGISTRY:-$(_tf ecr_registry)}"
export APP_URL="${APP_URL:-$(_tf app_url)}"

# shellcheck disable=SC1091
source "$EKS_DIR/versions.env"

# Fill the __PLACEHOLDERS__ used in addons/, platform/ and apps/.
render() {
  sed -e "s#__AWS_REGION__#${AWS_REGION}#g" \
      -e "s#__CLUSTER_NAME__#${CLUSTER_NAME}#g" \
      -e "s#__VPC_ID__#${VPC_ID}#g" \
      -e "s#__TARGET_GROUP_ARN__#${TARGET_GROUP_ARN}#g" \
      -e "s#__ECR_REGISTRY__#${ECR_REGISTRY}#g" \
      -e "s#__IMAGE_TAG__#${IMAGE_TAG:-latest}#g"
}

echo "cluster=${CLUSTER_NAME} region=${AWS_REGION} registry=${ECR_REGISTRY}"
