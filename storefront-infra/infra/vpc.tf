data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs      = slice(data.aws_availability_zones.available.names, 0, 3)
  app_fqdn = "${var.app_subdomain}.${var.domain_name}"

  # The Envoy (Gateway API) pods listen on 10080 for the HTTP listener on port 80.
  gateway_pod_port = 10080
}

# ─────────────────────────────────────────────────────────────────────
#  VPC — 3 AZs, three subnet tiers:
#    public    → ALB, NAT gateway
#    private   → EKS nodes and pods
#    database  → RDS, ElastiCache, Amazon MQ (no route to the internet)
# ─────────────────────────────────────────────────────────────────────
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.0"

  name = var.project
  cidr = var.vpc_cidr
  azs  = local.azs

  public_subnets   = [for i in range(3) : cidrsubnet(var.vpc_cidr, 8, i)]       # 10.20.0-2.0/24
  private_subnets  = [for i in range(3) : cidrsubnet(var.vpc_cidr, 4, i + 1)]   # 10.20.16/20, 32/20, 48/20 — room for pod IPs
  database_subnets = [for i in range(3) : cidrsubnet(var.vpc_cidr, 8, i + 100)] # 10.20.100-102.0/24

  create_database_subnet_group = true
  enable_nat_gateway           = true
  single_nat_gateway           = !var.high_availability # one NAT is cheaper; one per AZ survives an AZ outage
}
