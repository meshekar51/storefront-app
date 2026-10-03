# ─────────────────────────────────────────────────────────────────────
#  One Secrets Manager secret per backing service, holding everything a
#  pod needs to connect (endpoint + credentials). External Secrets
#  Operator copies them into Kubernetes Secrets in the storefront
#  namespace. Nothing secret is ever written to Git.
#
#  Note: these values are also in Terraform state — keep the state in an
#  encrypted S3 bucket with tight access (see versions.tf).
# ─────────────────────────────────────────────────────────────────────

resource "aws_secretsmanager_secret" "postgres" {
  name                    = "${var.project}/postgres"
  recovery_window_in_days = 0 # demo-friendly: destroy + re-create with the same name works
}

resource "aws_secretsmanager_secret_version" "postgres" {
  secret_id = aws_secretsmanager_secret.postgres.id
  secret_string = jsonencode({
    host     = aws_db_instance.postgres.address
    port     = tostring(aws_db_instance.postgres.port)
    dbname   = aws_db_instance.postgres.db_name
    username = aws_db_instance.postgres.username
    password = random_password.db.result
  })
}

resource "aws_secretsmanager_secret" "redis" {
  name                    = "${var.project}/redis"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "redis" {
  secret_id = aws_secretsmanager_secret.redis.id
  secret_string = jsonencode({
    host     = aws_elasticache_replication_group.redis.primary_endpoint_address
    port     = "6379"
    password = random_password.redis.result
  })
}

resource "aws_secretsmanager_secret" "rabbitmq" {
  name                    = "${var.project}/rabbitmq"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "rabbitmq" {
  secret_id = aws_secretsmanager_secret.rabbitmq.id
  secret_string = jsonencode({
    url      = aws_mq_broker.rabbitmq.instances[0].endpoints[0] # amqps://b-….mq.<region>.amazonaws.com:5671
    username = "storefront"
    password = random_password.mq.result
  })
}
