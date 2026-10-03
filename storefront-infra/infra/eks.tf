# ─────────────────────────────────────────────────────────────────────
#  EKS — managed node group in the private subnets.
#  The cluster add-ons Kubernetes needs to run are installed here; the
#  "platform" add-ons (load balancer controller, Envoy Gateway, External
#  Secrets) are Helm charts installed from the eks/ folder.
# ─────────────────────────────────────────────────────────────────────
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 21.0"

  name               = var.project
  kubernetes_version = var.kubernetes_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  endpoint_public_access       = true
  endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs

  # Whoever runs `terraform apply` becomes cluster admin.
  enable_cluster_creator_admin_permissions = true

  addons = {
    vpc-cni = {
      before_compute = true
      # Turns on enforcement of Kubernetes NetworkPolicy objects.
      configuration_values = jsonencode({ enableNetworkPolicy = "true" })
    }
    eks-pod-identity-agent = {
      before_compute = true # lets pods assume IAM roles (load balancer controller, External Secrets)
    }
    kube-proxy = {}
    coredns    = {}
  }

  eks_managed_node_groups = {
    default = {
      instance_types = var.node_instance_types
      min_size       = var.node_min_size
      max_size       = var.node_max_size
      desired_size   = var.node_desired_size
      # AL2023 is the module default AMI. Nodes get ECR pull permission by default.
    }
  }
}
