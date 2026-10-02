import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  useGetAdminOrderQuery,
  useUpdateAdminOrderStatusMutation,
} from '../app/api/adminOrderApi';
import { describeApiError } from '../app/api/authApi';
import type { OrderStatus } from '../app/api/orderApi';
import RequireAdmin from '../components/RequireAdmin';
import {
  ORDER_STATUS_LABELS,
  OrderStatusBadge,
  PaymentStatusBadge,
} from '../components/OrderStatusBadge';
import { formatDateTime } from '../components/OrderTimeline';
import { formatPrice } from '../mocks/catalog';

/**
 * The statuses an admin may move an order to. Derived from the label map that
 * mirrors the backend `OrderStatus` enum, so no value is invented. Whether a
 * specific move is legal is decided by the backend state machine — an
 * unsupported transition is reported here as a 409.
 */
const ORDER_STATUS_VALUES = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];

interface Feedback {
  kind: 'success' | 'error';
  text: string;
}

/** Admin order detail. Body is guarded by {@link RequireAdmin}. */
function AdminOrderDetailPage() {
  return (
    <RequireAdmin>
      <AdminOrderDetailContent />
    </RequireAdmin>
  );
}

function AdminOrderDetailContent() {
  const { id } = useParams();
  const orderId = Number(id);
  const validId = Number.isInteger(orderId) && orderId > 0;

  const orderQuery = useGetAdminOrderQuery(orderId, { skip: !validId });
  const [updateStatus, { isLoading: updating }] = useUpdateAdminOrderStatusMutation();

  const [target, setTarget] = useState<OrderStatus | ''>('');
  const [feedback, setFeedback] = useState<Feedback | null>(null);

  const handleUpdate = async (orderIdToUpdate: number) => {
    if (target === '') {
      return;
    }
    setFeedback(null);
    try {
      // The response is the refreshed order; invalidating its tag also refetches
      // the detail view and the admin list so both stay consistent.
      const updated = await updateStatus({ id: orderIdToUpdate, orderStatus: target }).unwrap();
      setFeedback({
        kind: 'success',
        text: `Order ${updated.orderNumber} is now ${ORDER_STATUS_LABELS[updated.orderStatus]}.`,
      });
      setTarget('');
    } catch (error) {
      setFeedback({
        kind: 'error',
        text: describeApiError(error, 'We could not update this order status.'),
      });
    }
  };

  if (!validId) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not read that order</h2>
          <p className="empty-state-text">The order reference in the link is not valid.</p>
          <div className="empty-state-actions">
            <Link to="/admin/orders" className="btn btn-primary">
              Back to order management
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
          <h2 className="empty-state-title">Loading order…</h2>
          <p className="empty-state-text">Fetching the order details.</p>
        </div>
      </section>
    );
  }

  if (orderQuery.isError) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load that order</h2>
          <p className="empty-state-text">
            {describeApiError(
              orderQuery.error,
              'That order does not exist, or your account cannot view it.',
            )}
          </p>
          <div className="empty-state-actions">
            <button type="button" className="btn btn-primary" onClick={() => orderQuery.refetch()}>
              Try again
            </button>
            <Link to="/admin/orders" className="btn btn-outline">
              Back to order management
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

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link to="/admin/orders">Admin</Link>
        <span aria-hidden="true">/</span>
        <span>{order.orderNumber}</span>
      </nav>

      <header className="page-header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1 className="page-title">Order {order.orderNumber}</h1>
          <p className="page-lead">
            Order #{order.id}, placed {formatDateTime(order.createdAt)}. Item details are the
            snapshots recorded at purchase.
          </p>
        </div>
        <div className="order-header-badges">
          <OrderStatusBadge status={order.orderStatus} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
      </header>

      {feedback && (
        <p className={feedback.kind === 'success' ? 'form-success' : 'form-alert'} role="status">
          {feedback.text}
        </p>
      )}

      <div className="order-panel">
        <h2 className="order-panel-title">Update status</h2>
        <p className="form-hint">
          The backend validates every move against the order lifecycle, and rejects a transition the
          current state does not allow.
        </p>
        <div className="admin-toolbar">
          <div className="form-field">
            <label className="form-label" htmlFor="admin-order-status-target">
              New status
            </label>
            <select
              id="admin-order-status-target"
              className="form-select"
              value={target}
              onChange={(event) => setTarget(event.target.value as OrderStatus | '')}
              disabled={updating}
            >
              <option value="">Select a status…</option>
              {ORDER_STATUS_VALUES.map((value) => (
                <option key={value} value={value}>
                  {ORDER_STATUS_LABELS[value]}
                </option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => handleUpdate(order.id)}
              disabled={updating || target === ''}
            >
              {updating ? 'Updating…' : 'Update status'}
            </button>
          </div>
        </div>
      </div>

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
          <h2 className="order-panel-title">Customer</h2>
          {/* The admin OrderResponse exposes the customer id only — no name or
              email is returned, so none is shown or invented here. */}
          <p className="cart-row-unit">Customer #{order.userId}</p>

          <h2 className="order-panel-title">Shipping address</h2>
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

          <h2 className="order-panel-title">Record</h2>
          <dl className="order-meta">
            <div>
              <dt>Order status</dt>
              <dd>
                <OrderStatusBadge status={order.orderStatus} />
              </dd>
            </div>
            <div>
              <dt>Payment status</dt>
              <dd>
                <PaymentStatusBadge status={order.paymentStatus} />
              </dd>
            </div>
            <div>
              <dt>Placed</dt>
              <dd>{formatDateTime(order.createdAt)}</dd>
            </div>
            <div>
              <dt>Last updated</dt>
              <dd>{formatDateTime(order.updatedAt)}</dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="empty-state-actions">
        <Link to="/admin/orders" className="btn btn-outline">
          Back to order management
        </Link>
      </div>
    </section>
  );
}

export default AdminOrderDetailPage;
