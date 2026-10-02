import { useSelector } from 'react-redux';
import { Link, useLocation, useParams } from 'react-router-dom';
import { describeApiError } from '../app/api/authApi';
import type { CheckoutResponse } from '../app/api/checkoutApi';
import { useGetOrderQuery } from '../app/api/orderApi';
import { useGetPaymentQuery } from '../app/api/paymentApi';
import type { RootState } from '../app/store';
import { OrderStatusBadge, PaymentStatusBadge } from '../components/OrderStatusBadge';
import { formatDateTime } from '../components/OrderTimeline';
import { formatPrice } from '../mocks/catalog';

interface ConfirmationLocationState {
  /** The full checkout result, forwarded by CheckoutPage. */
  checkout?: CheckoutResponse;
}

/**
 * Order confirmation page (Phase 5 client). Shows the outcome of the checkout
 * transaction for one order.
 *
 * The backend answers `POST /api/checkout` with 201 whether the sandbox charge
 * was approved or declined — a decline is a business outcome, not an error — so
 * this page branches on the returned `paymentStatus`/`orderStatus` and shows a
 * clear failure state instead of a success panel.
 *
 * The checkout result is taken from router state when the customer has just
 * placed the order, and otherwise refetched by id so a reload or a shared
 * confirmation link still works.
 */
function OrderConfirmationPage() {
  const { id } = useParams();
  const location = useLocation();
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));

  const orderId = Number(id);
  const validId = Number.isInteger(orderId) && orderId > 0;

  const locationState = location.state as ConfirmationLocationState | null;
  const prefetched = locationState?.checkout ?? null;

  // Both queries are skipped while the router state already carries the result,
  // so the hooks stay unconditional.
  const orderQuery = useGetOrderQuery(orderId, { skip: !isAuthenticated || !validId || !!prefetched });
  const paymentQuery = useGetPaymentQuery(orderId, {
    skip: !isAuthenticated || !validId || !!prefetched,
  });

  const order = prefetched?.order ?? orderQuery.data;
  const payment = prefetched?.payment ?? paymentQuery.data;

  if (!isAuthenticated) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Sign in to view this order</h2>
          <p className="empty-state-text">Orders belong to your account, so sign in to continue.</p>
          <div className="empty-state-actions">
            <Link to="/login" className="btn btn-primary">
              Sign in
            </Link>
          </div>
        </div>
      </section>
    );
  }

  if (!validId) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not read that order</h2>
          <p className="empty-state-text">The order reference in the link is not valid.</p>
          <div className="empty-state-actions">
            <Link to="/orders" className="btn btn-primary">
              Go to your orders
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const loading = !prefetched && (orderQuery.isLoading || paymentQuery.isLoading);
  const errored = !prefetched && orderQuery.isError;

  if (loading) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading your order…</h2>
          <p className="empty-state-text">Fetching the order we just placed.</p>
        </div>
      </section>
    );
  }

  if (errored) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load this order</h2>
          <p className="empty-state-text">
            {describeApiError(orderQuery.error, 'That order is not available on your account.')}
          </p>
          <div className="empty-state-actions">
            <button type="button" className="btn btn-primary" onClick={() => orderQuery.refetch()}>
              Try again
            </button>
            <Link to="/orders" className="btn btn-outline">
              Go to your orders
            </Link>
          </div>
        </div>
      </section>
    );
  }

  if (!order) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Order not found</h2>
          <p className="empty-state-text">
            We could not find an order with that reference on your account.
          </p>
          <div className="empty-state-actions">
            <Link to="/orders" className="btn btn-primary">
              Go to your orders
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const paid = order.paymentStatus === 'PAID';
  const declined = order.paymentStatus === 'FAILED';

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link to="/orders">Orders</Link>
        <span aria-hidden="true">/</span>
        <span>Confirmation</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">{paid ? 'Thank you for your order' : 'Order not completed'}</h1>
          <p className="page-lead">
            {paid
              ? 'Your payment was approved and your order is being prepared for dispatch.'
              : 'Your payment was declined, so the order was cancelled and nothing was charged.'}
          </p>
        </div>
      </header>

      {paid ? (
        <p className="form-success" role="status">
          Payment approved — order {order.orderNumber} is confirmed.
        </p>
      ) : (
        <p className="form-alert" role="alert">
          {declined
            ? 'The payment was declined by the payment provider. The order was cancelled, the reserved stock was released and your cart was left unchanged so you can try again.'
            : 'This order is not paid yet. Its current status is shown below.'}
        </p>
      )}

      <div className="order-detail-grid">
        <div className="order-panel">
          <h2 className="order-panel-title">Order</h2>
          <dl className="order-meta">
            <div>
              <dt>Order number</dt>
              <dd>{order.orderNumber}</dd>
            </div>
            <div>
              <dt>Order reference</dt>
              <dd>#{order.id}</dd>
            </div>
            <div>
              <dt>Placed</dt>
              <dd>{formatDateTime(order.createdAt)}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <OrderStatusBadge status={order.orderStatus} />
              </dd>
            </div>
            <div>
              <dt>Payment</dt>
              <dd>
                <PaymentStatusBadge status={order.paymentStatus} />
              </dd>
            </div>
          </dl>
        </div>

        <div className="order-panel">
          <h2 className="order-panel-title">Totals</h2>
          <dl className="order-meta">
            <div>
              <dt>Subtotal</dt>
              <dd>{formatPrice(order.subtotal)}</dd>
            </div>
            {order.discount > 0 && (
              <div>
                <dt>Discount</dt>
                <dd>-{formatPrice(order.discount)}</dd>
              </div>
            )}
            <div>
              <dt>Delivery</dt>
              <dd>{order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Free'}</dd>
            </div>
            <div>
              <dt>Total</dt>
              <dd>
                <strong>{formatPrice(order.totalAmount)}</strong>
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="order-panel">
        <h2 className="order-panel-title">Payment</h2>
        {payment ? (
          <dl className="order-meta">
            <div>
              <dt>Provider</dt>
              <dd>{payment.provider}</dd>
            </div>
            <div>
              <dt>Amount</dt>
              <dd>{formatPrice(payment.amount)}</dd>
            </div>
            <div>
              <dt>Reference</dt>
              <dd>{payment.providerPaymentId ?? 'Not supplied'}</dd>
            </div>
          </dl>
        ) : (
          <p className="form-hint">No payment record is available for this order.</p>
        )}
      </div>

      <div className="order-panel">
        <h2 className="order-panel-title">Items</h2>
        <ul className="order-summary-items">
          {order.items.map((item) => (
            <li key={item.id}>
              <span>
                {item.productName} &times; {item.quantity}
              </span>
              <strong>{formatPrice(item.lineTotal)}</strong>
            </li>
          ))}
        </ul>
      </div>

      <div className="empty-state-actions">
        <Link to={`/orders/${order.id}`} className="btn btn-primary">
          View order
        </Link>
        <Link to="/orders" className="btn btn-outline">
          All orders
        </Link>
        <Link to="/products" className="btn btn-outline">
          Continue shopping
        </Link>
      </div>
    </section>
  );
}

export default OrderConfirmationPage;
