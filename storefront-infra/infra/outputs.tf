# Everything the eks/ folder needs. Its scripts read these with
#   terraform -chdir=../infra output -json
output "region" {
  value = var.region
}

output "cluster_name" {
  value = module.eks.cluster_name
}

output "vpc_id" {
  value = module.vpc.vpc_id
}

output "app_url" {
  value = "https://${local.app_fqdn}"
}

output "alb_dns_name" {
  value = aws_lb.app.dns_name
}

output "gateway_target_group_arn" {
  description = "Bound to the Envoy Gateway Service by a TargetGroupBinding"
  value       = aws_lb_target_group.gateway.arn
}

output "ecr_registry" {
  value = split("/", aws_ecr_repository.app["frontend"].repository_url)[0]
}

output "secret_names" {
  value = {
    postgres = aws_secretsmanager_secret.postgres.name
    redis    = aws_secretsmanager_secret.redis.name
    rabbitmq = aws_secretsmanager_secret.rabbitmq.name
  }
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "mq_endpoint" {
  value = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
}

output "mq_console_url" {
  description = "Only reachable from inside the VPC (private broker)"
  value       = aws_mq_broker.rabbitmq.instances[0].console_url
}
