import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Link, useParams } from 'react-router-dom';
import { describeApiError } from '../app/api/authApi';
import {
  useCancelOrderMutation,
  useGetOrderQuery,
  useGetOrderTrackingQuery,
} from '../app/api/orderApi';
import { useGetPaymentQuery } from '../app/api/paymentApi';
import type { RootState } from '../app/store';
import { OrderStatusBadge, PaymentStatusBadge } from '../components/OrderStatusBadge';
import { OrderTimeline, formatDateTime } from '../components/OrderTimeline';
import { formatPrice } from '../mocks/catalog';

/**
 * Order detail page (Phase 5 client). Shows one customer's order: the item
 * snapshots captured at purchase, the money breakdown, the shipping address as
 * it was recorded at checkout, the payment record and the current tracking
 * position.
 *
 * Cancellation is offered only when the backend's tracking response reports the
 * order as still cancellable, and the mutation invalidates the order, tracking
 * and history tags so every view refreshes from the new state.
 */
function OrderDetailPage() {
  const { id } = useParams();
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));

  const orderId = Number(id);
  const validId = Number.isInteger(orderId) && orderId > 0;
  const canQuery = isAuthenticated && validId;

  const orderQuery = useGetOrderQuery(orderId, { skip: !canQuery });
  const trackingQuery = useGetOrderTrackingQuery(orderId, { skip: !canQuery });
  // The payment is supplementary: the order already carries the payment status,
  // so a missing/failed payment read must not take the whole page down.
  const paymentQuery = useGetPaymentQuery(orderId, { skip: !canQuery });

  const [cancelOrder, { isLoading: cancelling }] = useCancelOrderMutation();
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

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

  if (orderQuery.isLoading) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading your order…</h2>
          <p className="empty-state-text">Fetching the details for this order.</p>
        </div>
      </section>
    );
  }

  if (orderQuery.isError) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not find that order</h2>
          <p className="empty-state-text">
            {describeApiError(
              orderQuery.error,
              'That order does not exist, or it belongs to another account.',
            )}
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

  const order = orderQuery.data;
  if (!order) {
    return null;
  }

  const tracking = trackingQuery.data;
  const payment = paymentQuery.isSuccess ? paymentQuery.data : undefined;
  const cancellable = tracking?.cancellable === true;

  const handleCancel = async () => {
    setActionError(null);
    try {
      await cancelOrder(order.id).unwrap();
      setConfirmingCancel(false);
    } catch (error) {
      setActionError(
        describeApiError(error, 'We could not cancel this order. Please try again.'),
      );
    }
  };

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link to="/orders">Orders</Link>
        <span aria-hidden="true">/</span>
        <span>{order.orderNumber}</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">Order {order.orderNumber}</h1>
          <p className="page-lead">
            Placed {formatDateTime(order.createdAt)} — these are the item details recorded at
            purchase, so later catalogue changes never alter this order.
          </p>
        </div>
        <div className="order-header-badges">
          <OrderStatusBadge status={order.orderStatus} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
      </header>

      {actionError && (
        <p className="form-alert" role="alert">
          {actionError}
        </p>
      )}

      {tracking ? (
        <div className="order-panel">
          <OrderTimeline tracking={tracking} />
        </div>
      ) : null}

      <div className="order-detail-grid">
        <div className="order-panel">
          <h2 className="order-panel-title">Items</h2>
          <ul className="order-summary-items">
            {order.items.map((item) => (
              <li key={item.id}>
                <span>
                  {item.productName} &times; {item.quantity}
                  <small className="order-item-meta">
                    SKU {item.sku} — {formatPrice(item.priceAtPurchase)} each
                  </small>
                </span>
                <strong>{formatPrice(item.lineTotal)}</strong>
              </li>
            ))}
          </ul>

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

        <div className="order-panel">
          <h2 className="order-panel-title">Delivery address</h2>
          <address className="order-address">
            <strong>{order.recipientName}</strong>
            <br />
            {order.addressLine1}
            {order.addressLine2 && (
              <>
                <br />
                {order.addressLine2}
              </>
            )}
            <br />
            {order.city}, {order.state} {order.postalCode}
            <br />
            {order.country}
            <br />
            {order.phone}
          </address>

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
              <div>
                <dt>Status</dt>
                <dd>
                  <PaymentStatusBadge status={payment.paymentStatus} />
                </dd>
              </div>
            </dl>
          ) : (
            <p className="form-hint">
              {paymentQuery.isLoading
                ? 'Loading payment details…'
                : 'No payment record is available for this order.'}
            </p>
          )}
        </div>
      </div>

      <div className="order-panel">
        <h2 className="order-panel-title">Need to change this order?</h2>
        {cancellable ? (
          confirmingCancel ? (
            <div className="order-confirm">
              <p>
                Cancel order {order.orderNumber}? Orders that have already shipped cannot be
                cancelled.
              </p>
              <div className="order-confirm-actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleCancel}
                  disabled={cancelling}
                >
                  {cancelling ? 'Cancelling…' : 'Yes, cancel this order'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setConfirmingCancel(false)}
                  disabled={cancelling}
                >
                  Keep my order
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setConfirmingCancel(true)}
            >
              Cancel order
            </button>
          )
        ) : (
          <p className="form-hint">
            This order can no longer be cancelled from your account. Contact support if it has not
            shipped yet.
          </p>
        )}
      </div>
    </section>
  );
}

export default OrderDetailPage;
