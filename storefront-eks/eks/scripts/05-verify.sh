#!/usr/bin/env bash
# End-to-end check through the real front door: Route 53 → ALB/WAF → Gateway → services.
set -euo pipefail
source "$(dirname "$0")/00-env.sh"

ok()   { printf '  ✅ %s\n' "$1"; }
fail() { printf '  ❌ %s\n' "$1"; exit 1; }

echo "▶ cluster"
kubectl get pods -n storefront -o wide
kubectl get httproute -n storefront
echo
echo "▶ ALB targets (Envoy pods registered by the TargetGroupBinding)"
aws elbv2 describe-target-health --target-group-arn "$TARGET_GROUP_ARN" \
  --query 'TargetHealthDescriptions[].[Target.Id,TargetHealth.State]' --output table

echo "▶ HTTP checks against ${APP_URL}"
code=$(curl -s -o /dev/null -w '%{http_code}' "${APP_URL}/")
[[ "$code" == "200" ]] && ok "frontend /" || fail "frontend returned $code"

n=$(curl -sf "${APP_URL}/api/products" | jq length)
[[ "$n" -gt 0 ]] && ok "catalogue: $n products" || fail "no products"

CART=$(uuidgen | tr '[:upper:]' '[:lower:]')
curl -sf -XPOST "${APP_URL}/api/carts/${CART}/items" -H 'content-type: application/json' \
  -d '{"productId":2,"quantity":1}' >/dev/null && ok "cart: item added" || fail "cart"

status=$(curl -sf -XPOST "${APP_URL}/api/orders" -H 'content-type: application/json' \
  -d "{\"cartId\":\"${CART}\",\"paymentToken\":\"tok_visa\"}" | jq -r .status)
[[ "$status" == "CONFIRMED" ]] && ok "order: CONFIRMED" || fail "order status: $status"

code=$(curl -s -o /dev/null -w '%{http_code}' -XPOST "${APP_URL}/api/payments" -H 'content-type: application/json' -d '{}')
[[ "$code" == "404" ]] && ok "payment is not reachable from the internet (404)" || fail "payment exposed? got $code"

echo
echo "▶ notification (last log lines — should show the email for the order above)"
sleep 3
kubectl logs -n storefront deploy/notification --tail=3
