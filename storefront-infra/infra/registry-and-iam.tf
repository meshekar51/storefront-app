# ─────────────────────────────────────────────────────────────────────
#  Container registries + IAM for the two in-cluster controllers.
# ─────────────────────────────────────────────────────────────────────

locals {
  images = [
    "catalogue-service",
    "cart-service",
    "inventory-service",
    "payment-service",
    "order-service",
    "notification-service",
    "frontend",
  ]
}

resource "aws_ecr_repository" "app" {
  for_each = toset(local.images)

  name                 = "${var.project}/${each.key}"
  image_tag_mutability = "IMMUTABLE" # a tag always means the same bytes
  force_delete         = true        # demo-friendly destroy

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  for_each   = aws_ecr_repository.app
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 30 images"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 30 }
      action       = { type = "expire" }
    }]
  })
}

# ── EKS Pod Identity roles ───────────────────────────────────────────
# AWS Load Balancer Controller: registers Envoy pod IPs in the ALB target group.
module "lbc_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                            = "${var.project}-aws-lb-controller"
  attach_aws_lb_controller_policy = true

  associations = {
    controller = {
      cluster_name    = module.eks.cluster_name
      namespace       = "kube-system"
      service_account = "aws-load-balancer-controller"
    }
  }
}

# External Secrets Operator: reads ONLY the three storefront secrets.
module "eso_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                           = "${var.project}-external-secrets"
  attach_external_secrets_policy = true
  external_secrets_secrets_manager_arns = [
    aws_secretsmanager_secret.postgres.arn,
    aws_secretsmanager_secret.redis.arn,
    aws_secretsmanager_secret.rabbitmq.arn,
  ]

  associations = {
    controller = {
      cluster_name    = module.eks.cluster_name
      namespace       = "external-secrets"
      service_account = "external-secrets"
    }
  }
}
