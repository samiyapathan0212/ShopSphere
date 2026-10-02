import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import {
  useClearCartMutation,
  useGetCartQuery,
  useRemoveItemMutation,
  useUpdateItemQuantityMutation,
} from '../app/api/cartApi';
import { describeApiError } from '../app/api/authApi';
import type { RootState } from '../app/store';
import { formatPrice, getTrendingProducts } from '../mocks/catalog';

/**
 * Shopping cart page. Loads the customer's cart from the cart API — the cart
 * requires authentication, so signed-out visitors get a sign-in call to action.
 * Quantity changes, item removal and clearing use the existing cart endpoints;
 * every mutation returns the cart, so rows, totals and the navbar counter stay
 * in step. The "trending" suggestions remain mock-catalog based.
 */
function CartPage() {
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const cartQuery = useGetCartQuery(undefined, { skip: !isAuthenticated });
  const [updateQuantity, { isLoading: updating }] = useUpdateItemQuantityMutation();
  const [removeItem, { isLoading: removing }] = useRemoveItemMutation();
  const [clearCart, { isLoading: clearing }] = useClearCartMutation();
  // While one row's mutation is in flight, every other control is disabled so
  // overlapping cart mutations cannot be triggered accidentally.
  const [rowBusy, setRowBusy] = useState<number | 'cart' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const suggestions = getTrendingProducts(4);

  const changeQuantity = async (productId: number, quantity: number) => {
    setActionError(null);
    setRowBusy(productId);
    try {
      await updateQuantity({ productId, body: { quantity } }).unwrap();
    } catch (error) {
      setActionError(describeApiError(error, 'Could not update the quantity.'));
    } finally {
      setRowBusy(null);
    }
  };

  const remove = async (productId: number) => {
    setActionError(null);
    setRowBusy(productId);
    try {
      await removeItem(productId).unwrap();
    } catch (error) {
      setActionError(describeApiError(error, 'Could not remove this item.'));
    } finally {
      setRowBusy(null);
    }
  };

  const clear = async () => {
    setActionError(null);
    setRowBusy('cart');
    try {
      await clearCart().unwrap();
    } catch (error) {
      setActionError(describeApiError(error, 'Could not clear the cart.'));
    } finally {
      setRowBusy(null);
    }
  };

  const cart = cartQuery.data;
  const busy = rowBusy !== null || updating || removing || clearing;

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <span>Cart</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">Shopping cart</h1>
          <p className="page-lead">
            Products stay in your cart between visits; quantities and prices come straight from the
            catalogue service.
          </p>
        </div>
      </header>

      {!isAuthenticated ? (
        <div className="empty-state-card empty-state-card--centered">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <path
                d="M3 4h2.2l2.2 10.4a2 2 0 0 0 2 1.6h7.3a2 2 0 0 0 2-1.5L20 7H6.2"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="10" cy="20" r="1.4" fill="currentColor" />
              <circle cx="17" cy="20" r="1.4" fill="currentColor" />
            </svg>
          </span>
          <h2 className="empty-state-title">Sign in to view your cart</h2>
          <p className="empty-state-text">
            Carts belong to customer accounts, so once you are signed in your items follow you
            across devices and visits.
          </p>
          <div className="empty-state-actions">
            <Link to="/login" className="btn btn-primary">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-outline">
              Create an account
            </Link>
          </div>
        </div>
      ) : cartQuery.isLoading ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading your cart…</h2>
          <p className="empty-state-text">Checking the cart service for your saved items.</p>
        </div>
      ) : cartQuery.isError ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load your cart</h2>
          <p className="empty-state-text">
            {describeApiError(cartQuery.error, 'The cart service is not responding right now.')}
          </p>
          <div className="empty-state-actions">
            <button type="button" className="btn btn-primary" onClick={() => cartQuery.refetch()}>
              Try again
            </button>
            <Link to="/products" className="btn btn-outline">
              Browse products
            </Link>
          </div>
        </div>
      ) : !cart || cart.items.length === 0 ? (
        <div className="empty-state-card empty-state-card--centered">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <path
                d="M3 4h2.2l2.2 10.4a2 2 0 0 0 2 1.6h7.3a2 2 0 0 0 2-1.5L20 7H6.2"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="10" cy="20" r="1.4" fill="currentColor" />
              <circle cx="17" cy="20" r="1.4" fill="currentColor" />
            </svg>
          </span>
          <h2 className="empty-state-title">Your cart is empty</h2>
          <p className="empty-state-text">
            Browse the catalogue and add something you like. Free delivery applies to orders over
            $50, and every order includes 30-day returns.
          </p>
          <div className="empty-state-actions">
            <Link to="/products" className="btn btn-primary">
              Start shopping
            </Link>
            <Link to="/wishlist" className="btn btn-outline">
              View wishlist
            </Link>
          </div>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="cart-main">
            {actionError && (
              <p className="form-alert" role="alert">
                {actionError}
              </p>
            )}

            <ul className="cart-list">
              {cart.items.map((item) => (
                <li key={item.productId} className="cart-row">
                  <div className="cart-row-info">
                    <p className="cart-row-name">
                      <Link to={`/products/${item.productId}`}>{item.productName}</Link>
                    </p>
                    <p className="cart-row-sku">SKU {item.productSku}</p>
                    <p className="cart-row-unit">{formatPrice(item.unitPrice)} each</p>
                  </div>

                  <div className="cart-row-controls">
                    <div className="qty-selector" aria-label={`Quantity for ${item.productName}`}>
                      <button
                        type="button"
                        className="qty-btn"
                        onClick={() => changeQuantity(item.productId, item.quantity - 1)}
                        disabled={busy || item.quantity <= 1}
                        aria-label="Decrease quantity"
                      >
                        &minus;
                      </button>
                      <input
                        className="qty-input"
                        type="number"
                        min={1}
                        value={item.quantity}
                        readOnly
                        aria-label={`Quantity of ${item.productName} in cart`}
                      />
                      <button
                        type="button"
                        className="qty-btn"
                        onClick={() => changeQuantity(item.productId, item.quantity + 1)}
                        disabled={busy}
                        aria-label="Increase quantity"
                      >
                        +
                      </button>
                    </div>

                    <p className="cart-row-price">{formatPrice(item.subtotal)}</p>

                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={() => remove(item.productId)}
                      disabled={busy}
                    >
                      Remove
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <aside className="cart-summary" aria-label="Order summary">
            <h2 className="cart-summary-title">Summary</h2>
            <div className="cart-summary-row">
              <span>Items</span>
              <strong>{cart.itemCountWithQuantity}</strong>
            </div>
            <div className="cart-summary-row cart-summary-total">
              <span>Subtotal</span>
              <strong>{formatPrice(cart.subtotal)}</strong>
            </div>
            <p className="cart-summary-note">Delivery and payment are handled at checkout.</p>
            <Link to="/checkout" className="btn btn-primary btn-block">
              Proceed to checkout
            </Link>
            <Link to="/products" className="btn btn-outline btn-block">
              Continue shopping
            </Link>
            <button
              type="button"
              className="btn btn-ghost btn-sm btn-block"
              onClick={clear}
              disabled={busy}
            >
              Clear cart
            </button>
          </aside>
        </div>
      )}

      <section className="section" aria-labelledby="cart-suggestions-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">Popular right now</p>
            <h2 id="cart-suggestions-title" className="section-title">
              Trending this week
            </h2>
          </div>
          <Link to="/products" className="link-arrow">
            All products
          </Link>
        </header>
        <ul className="product-grid product-grid--four">
          {suggestions.map((product) => (
            <li key={product.id}>
              <ProductCard product={product} />
            </li>
          ))}
        </ul>
      </section>
    </section>
  );
}

export default CartPage;
