# Storefront — application code

Six Spring Boot 4 services (Java 21) and a React frontend. One Maven build,
one Dockerfile for every Java service, one `docker compose up` to run it all.

```
app/
├── pom.xml                      parent: Spring Boot 4.1, Java 21
├── Dockerfile                   any Java service: --build-arg SERVICE=<name>
├── docker-compose.yml           whole system on your laptop
├── local/gateway.conf           local stand-in for the Gateway API routes
├── scripts/build-and-push.sh    build all 7 images → ECR
├── frontend/                    React 19 + Vite, served by nginx
└── services/
    ├── catalogue-service/       products        PostgreSQL + Redis cache
    ├── cart-service/            carts           PostgreSQL
    ├── inventory-service/       stock           PostgreSQL (atomic reserve)
    ├── payment-service/         payments        PostgreSQL + fake provider
    ├── order-service/           checkout        PostgreSQL, calls 4 services, publishes to MQ
    └── notification-service/    emails (logged) consumes from MQ
```

## Services at a glance

| Service | Public path (via gateway) | Talks to | Data |
|---|---|---|---|
| catalogue | `GET /api/products`, `GET /api/products/{id}` | — | schema `catalogue`, Redis cache (10 min TTL) |
| cart | `/api/carts/{cartId}/…` | catalogue | schema `cart` |
| inventory | `GET /api/inventory` only | — | schema `inventory` |
| order | `POST /api/orders`, `GET /api/orders/{ref}` | cart, catalogue, inventory, payment, MQ | schema `ordering` |
| payment | **internal only** | fake provider | schema `payment` |
| notification | **no HTTP API** | MQ | — |

All services: port `8080`, health at `/actuator/health/liveness` and `/actuator/health/readiness`.

One PostgreSQL database, **one schema per service**. Flyway creates each schema and its
tables on startup, so there is nothing to run by hand. A service never reads another
service's schema — it calls its API.

## Run it locally

```bash
docker compose up --build
```

| URL | What |
|---|---|
| http://localhost:8080 | the shop |
| http://localhost:15672 | RabbitMQ console (`storefront` / `storefront`) |

First start takes a few minutes (Maven downloads). Then try:

1. Add a few products, open **Cart**, place the order with the approving test card → green banner.
2. Watch the email go out: `docker compose logs -f notification`
3. Place another order with the **declining** card → "Payment declined", stock is given back.
4. Buy 4 × "Notebook A5" (only 3 in stock) → "Out of stock".

### With curl

```bash
CART=$(uuidgen)
curl -s localhost:8080/api/products | jq '.[0]'
curl -s -XPOST localhost:8080/api/carts/$CART/items -H 'content-type: application/json' -d '{"productId":1,"quantity":2}'
curl -s -XPOST localhost:8080/api/orders -H 'content-type: application/json' -d "{\"cartId\":\"$CART\",\"paymentToken\":\"tok_visa\"}" | jq
```

### Frontend hot reload

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173, /api proxied to :8080
```

## Configuration (environment variables)

Every value has a local default, so compose needs very little. On EKS these come from
Kubernetes Secrets synced from AWS Secrets Manager.

| Variable | Used by | Example on AWS |
|---|---|---|
| `DB_URL` | all except notification | `jdbc:postgresql://<rds>:5432/storefront?sslmode=require&currentSchema=cart` |
| `DB_USERNAME`, `DB_PASSWORD` | all except notification | from Secrets Manager |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` | catalogue | ElastiCache primary endpoint, `REDIS_SSL=true` |
| `MQ_URL`, `MQ_USERNAME`, `MQ_PASSWORD` | order, notification | `amqps://b-xxxx.mq.eu-central-1.amazonaws.com:5671` |
| `CATALOGUE_URL`, `CART_URL`, `INVENTORY_URL`, `PAYMENT_URL` | cart, order | `http://catalogue.storefront.svc.cluster.local` |

## Push images to ECR

After the infra is applied (it creates the ECR repositories):

```bash
export AWS_REGION=eu-central-1
./scripts/build-and-push.sh          # tag = git short SHA
```
