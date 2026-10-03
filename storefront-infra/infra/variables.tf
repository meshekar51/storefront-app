variable "project" {
  description = "Name prefix for every resource"
  type        = string
  default     = "storefront"
}

variable "region" {
  type    = string
  default = "eu-central-1"
}

# ── DNS ──────────────────────────────────────────────────────────────
variable "domain_name" {
  description = "Existing public Route 53 hosted zone, e.g. example.com"
  type        = string
}

variable "app_subdomain" {
  description = "The shop is served at <app_subdomain>.<domain_name>"
  type        = string
  default     = "shop"
}

# ── Network / cluster ────────────────────────────────────────────────
variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "kubernetes_version" {
  type    = string
  default = "1.35"
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "Who may reach the Kubernetes API. Narrow this to your office / VPN IPs."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.large"]
}

variable "node_min_size" {
  type    = number
  default = 2
}

variable "node_max_size" {
  type    = number
  default = 4
}

variable "node_desired_size" {
  type    = number
  default = 3
}

# ── Data stores ──────────────────────────────────────────────────────
variable "postgres_version" {
  description = "RDS PostgreSQL major version"
  type        = string
  default     = "17"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.small"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "mq_instance_type" {
  description = "mq.t3.micro is deprecated for new brokers; mq.m7g.medium is the smallest current type"
  type        = string
  default     = "mq.m7g.medium"
}

variable "high_availability" {
  description = "true = Multi-AZ RDS, Redis replica with failover, 3-node MQ cluster (≈3× cost)"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Protect RDS from `terraform destroy`. Turn on for anything that matters."
  type        = bool
  default     = false
}
