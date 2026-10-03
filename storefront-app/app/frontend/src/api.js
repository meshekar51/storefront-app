// Every call goes to a relative /api/... path. In the browser that hits the
// same host that served the page: ALB → Gateway API → the right service.

async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body.detail || body.message || `Request failed (${res.status})`);
  }
  return body;
}

export function getCartId() {
  const key = "storefront.cartId";
  let id = localStorage.getItem(key);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(key, id);
  }
  return id;
}

export const api = {
  products: () => request("/api/products"),
  stock: () => request("/api/inventory"),
  cart: (cartId) => request(`/api/carts/${cartId}`),
  addToCart: (cartId, productId, quantity = 1) =>
    request(`/api/carts/${cartId}/items`, {
      method: "POST",
      body: JSON.stringify({ productId, quantity }),
    }),
  removeFromCart: (cartId, productId) =>
    request(`/api/carts/${cartId}/items/${productId}`, { method: "DELETE" }),
  checkout: (cartId, paymentToken) =>
    request("/api/orders", {
      method: "POST",
      body: JSON.stringify({ cartId, paymentToken }),
    }),
  order: (orderRef) => request(`/api/orders/${orderRef}`),
};
