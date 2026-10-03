# ─────────────────────────────────────────────────────────────────────
#  Data stores: RDS PostgreSQL · ElastiCache Redis · Amazon MQ (RabbitMQ)
#  All private, all encrypted in transit and at rest.
# ─────────────────────────────────────────────────────────────────────

# ── RDS PostgreSQL ───────────────────────────────────────────────────
# One database "storefront"; every service owns its own schema in it
# (catalogue, cart, inventory, payment, ordering), created by Flyway.
resource "random_password" "db" {
  length  = 32
  special = false
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.project}-postgres"
  engine         = "postgres"
  engine_version = var.postgres_version
  instance_class = var.db_instance_class

  db_name  = "storefront"
  username = "storefront"
  password = random_password.db.result

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = module.vpc.database_subnet_group_name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = var.high_availability

  backup_retention_period    = 7
  auto_minor_version_upgrade = true
  apply_immediately          = true

  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "${var.project}-postgres-final" : null
}

# ── ElastiCache Redis (catalogue cache) ──────────────────────────────
resource "random_password" "redis" {
  length  = 32
  special = false
}

resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.project}-redis"
  subnet_ids = module.vpc.database_subnets
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.project}-redis"
  description          = "Catalogue cache"

  engine               = "redis"
  engine_version       = "7.1"
  parameter_group_name = "default.redis7"
  node_type            = var.redis_node_type
  port                 = 6379

  num_cache_clusters         = var.high_availability ? 2 : 1
  automatic_failover_enabled = var.high_availability
  multi_az_enabled           = var.high_availability

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true # clients must use TLS (REDIS_SSL=true)
  auth_token                 = random_password.redis.result
}

# ── Amazon MQ for RabbitMQ (order events) ────────────────────────────
resource "random_password" "mq" {
  length  = 32
  special = false # Amazon MQ rejects , : = in passwords
}

resource "aws_mq_broker" "rabbitmq" {
  broker_name        = "${var.project}-rabbitmq"
  engine_type        = "RabbitMQ"
  engine_version     = "4.2"
  host_instance_type = var.mq_instance_type
  deployment_mode    = var.high_availability ? "CLUSTER_MULTI_AZ" : "SINGLE_INSTANCE"

  publicly_accessible        = false
  subnet_ids                 = var.high_availability ? module.vpc.database_subnets : [module.vpc.database_subnets[0]]
  security_groups            = [aws_security_group.mq.id]
  auto_minor_version_upgrade = true

  user {
    username = "storefront"
    password = random_password.mq.result
  }

  logs {
    general = true
  }
}
