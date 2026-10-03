import { useCallback, useEffect, useState } from "react";
import { api, getCartId } from "./api.js";

const money = (value, currency = "EUR") =>
  new Intl.NumberFormat("en-NL", { style: "currency", currency }).format(Number(value ?? 0));

const CATEGORY_TONE = {
  Audio: "tone-audio",
  Kitchen: "tone-kitchen",
  Outdoor: "tone-outdoor",
  Desk: "tone-desk",
};

export default function App() {
  const cartId = getCartId();
  const [products, setProducts] = useState([]);
  const [stock, setStock] = useState({});
  const [cart, setCart] = useState({ items: [], total: 0 });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [order, setOrder] = useState(null);
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  const refreshStock = useCallback(async () => {
    const levels = await api.stock();
    setStock(Object.fromEntries(levels.map((s) => [s.productId, s.available])));
  }, []);

  useEffect(() => {
    Promise.all([api.products(), api.cart(cartId), refreshStock()])
      .then(([p, c]) => {
        setProducts(p);
        setCart(c);
      })
      .catch((e) => setError(e.message));
  }, [cartId, refreshStock]);

  async function add(productId) {
    setBusy(`add-${productId}`);
    setError(null);
    try {
      setCart(await api.addToCart(cartId, productId));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(null);
    }
  }

  async function remove(productId) {
    setBusy(`remove-${productId}`);
    try {
      setCart(await api.removeFromCart(cartId, productId));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(null);
    }
  }

  async function checkout(paymentToken) {
    setBusy("checkout");
    setError(null);
    try {
      const placed = await api.checkout(cartId, paymentToken);
      setOrder(placed);
      setDrawerOpen(false);
      setCart(await api.cart(cartId));
      await refreshStock();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(null);
    }
  }

  const count = cart.items.reduce((n, i) => n + i.quantity, 0);

  return (
    <div className="page">
      <header className="topbar">
        <a className="brand" href="/">
          storefront<span>.</span>
        </a>
        <button className="cart-button" onClick={() => setDrawerOpen(true)} aria-label={`Open cart, ${count} items`}>
          Cart
          <span className="badge">{count}</span>
        </button>
      </header>

      <main>
        <section className="intro">
          <h1>Everyday things, well made.</h1>
          <p>Browse, add to cart and check out. Every click is a real request through the gateway to a Java service.</p>
        </section>

        {error && (
          <div className="notice error" role="alert">
            {error}
            <button onClick={() => setError(null)} aria-label="Dismiss">×</button>
          </div>
        )}

        {order && <OrderResult order={order} onClose={() => setOrder(null)} />}

        <section className="grid" aria-label="Products">
          {products.map((p) => {
            const left = stock[p.id];
            const soldOut = left === 0;
            return (
              <article key={p.id} className="card">
                <div className={`thumb ${CATEGORY_TONE[p.category] ?? ""}`} aria-hidden="true">
                  <span>{p.name.slice(0, 1)}</span>
                </div>
                <div className="card-body">
                  <div className="meta">
                    <span>{p.category}</span>
                    <span className="sku">{p.sku}</span>
                  </div>
                  <h2>{p.name}</h2>
                  <p>{p.description}</p>
                </div>
                <div className="card-foot">
                  <div>
                    <strong>{money(p.price, p.currency)}</strong>
                    <small className={left !== undefined && left < 5 ? "low" : ""}>
                      {left === undefined ? "" : soldOut ? "Sold out" : `${left} in stock`}
                    </small>
                  </div>
                  <button
                    className="primary"
                    disabled={soldOut || busy === `add-${p.id}`}
                    onClick={() => add(p.id)}
                  >
                    {busy === `add-${p.id}` ? "Adding…" : "Add"}
                  </button>
                </div>
              </article>
            );
          })}
        </section>
      </main>

      {drawerOpen && (
        <CartDrawer
          cart={cart}
          busy={busy}
          onClose={() => setDrawerOpen(false)}
          onRemove={remove}
          onCheckout={checkout}
        />
      )}

      <footer className="footer">
        Cart <code>{cartId.slice(0, 8)}</code> · Route 53 → ALB + WAF → Gateway API → Service → Pod
      </footer>
    </div>
  );
}

function CartDrawer({ cart, busy, onClose, onRemove, onCheckout }) {
  const [token, setToken] = useState("tok_visa");
  const empty = cart.items.length === 0;

  return (
    <div className="scrim" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Shopping cart">
        <div className="drawer-head">
          <h2>Your cart</h2>
          <button className="ghost" onClick={onClose} aria-label="Close cart">×</button>
        </div>

        {empty ? (
          <p className="muted">Your cart is empty.</p>
        ) : (
          <ul className="lines">
            {cart.items.map((i) => (
              <li key={i.productId}>
                <div>
                  <strong>{i.productName}</strong>
                  <small>
                    {i.quantity} × {money(i.unitPrice)}
                  </small>
                </div>
                <div className="line-end">
                  <span>{money(i.lineTotal)}</span>
                  <button
                    className="link"
                    disabled={busy === `remove-${i.productId}`}
                    onClick={() => onRemove(i.productId)}
                  >
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="total">
          <span>Total</span>
          <strong>{money(cart.total)}</strong>
        </div>

        <fieldset className="pay" disabled={empty}>
          <legend>Payment (test mode)</legend>
          <label>
            <input
              type="radio"
              id="tok-visa"
              name="token"
              checked={token === "tok_visa"}
              onChange={() => setToken("tok_visa")}
            />
            Test card that <b>approves</b>
          </label>
          <label>
            <input
              type="radio"
              id="tok-decline"
              name="token"
              checked={token === "tok_decline"}
              onChange={() => setToken("tok_decline")}
            />
            Test card that <b>declines</b>
          </label>
        </fieldset>

        <button className="primary wide" disabled={empty || busy === "checkout"} onClick={() => onCheckout(token)}>
          {busy === "checkout" ? "Placing order…" : "Place order"}
        </button>
      </aside>
    </div>
  );
}

function OrderResult({ order, onClose }) {
  const ok = order.status === "CONFIRMED";
  return (
    <section className={`notice ${ok ? "success" : "error"}`} aria-live="polite">
      <div>
        <strong>
          {ok ? "Order confirmed" : "Order not placed"} · {order.orderRef}
        </strong>
        <p>
          {ok
            ? `${order.items.length} item(s), ${money(order.total, order.currency)}. A confirmation is on its way (via Amazon MQ → notification service).`
            : `${order.statusReason}. Nothing was charged and your cart is unchanged.`}
        </p>
      </div>
      <button onClick={onClose} aria-label="Dismiss">×</button>
    </section>
  );
}
