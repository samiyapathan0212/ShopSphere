import { useState } from 'react';
import { Link } from 'react-router-dom';
import { describeApiError } from '../app/api/authApi';
import { useGetAdminOrdersQuery } from '../app/api/adminOrderApi';
import type { OrderStatus } from '../app/api/orderApi';
import Pagination from '../components/Pagination';
import RequireAdmin from '../components/RequireAdmin';
import {
  ORDER_STATUS_LABELS,
  OrderStatusBadge,
  PaymentStatusBadge,
} from '../components/OrderStatusBadge';
import { formatDateTime } from '../components/OrderTimeline';
import { formatPrice } from '../mocks/catalog';

const PAGE_SIZE = 20;

/**
 * The statuses offered by the filter and the update control. Derived from the
 * label map that mirrors the backend `OrderStatus` enum, so the UI can never
 * offer a value the API would reject with a 400.
 */
const ORDER_STATUS_VALUES = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];

type StatusFilter = OrderStatus | '';

/**
 * Admin order list. Wrapped in {@link RequireAdmin}; the guarded body lives in
 * {@link AdminOrdersContent} so a non-admin never triggers an admin request.
 */
function AdminOrdersPage() {
  return (
    <RequireAdmin>
      <AdminOrdersContent />
    </RequireAdmin>
  );
}

function AdminOrdersContent() {
  // The shared Pagination control is 1-based; the backend pages are 0-based.
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<StatusFilter>('');

  const ordersQuery = useGetAdminOrdersQuery({
    page: page - 1,
    size: PAGE_SIZE,
    status: status === '' ? null : status,
  });

  const orders = ordersQuery.data?.content ?? [];
  const totalPages = ordersQuery.data?.totalPages ?? 0;
  const totalElements = ordersQuery.data?.totalElements ?? 0;

  // Changing the filter narrows the result set, so always return to page 1.
  const changeStatus = (value: StatusFilter) => {
    setStatus(value);
    setPage(1);
  };

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <span>Admin</span>
        <span aria-hidden="true">/</span>
        <span>Orders</span>
      </nav>

      <header className="page-header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1 className="page-title">Order management</h1>
          <p className="page-lead">
            Every order across all customers, newest first. Open an order to review its items and
            move it through the delivery lifecycle.
          </p>
        </div>
      </header>

      <div className="admin-toolbar">
        <div className="form-field">
          <label className="form-label" htmlFor="admin-order-status-filter">
            Filter by status
          </label>
          <select
            id="admin-order-status-filter"
            className="form-select"
            value={status}
            onChange={(event) => changeStatus(event.target.value as StatusFilter)}
          >
            <option value="">All statuses</option>
            {ORDER_STATUS_VALUES.map((value) => (
              <option key={value} value={value}>
                {ORDER_STATUS_LABELS[value]}
              </option>
            ))}
          </select>
        </div>
        <p className="form-hint">
          {totalElements} {totalElements === 1 ? 'order' : 'orders'}
          {status ? ` with status ${ORDER_STATUS_LABELS[status]}` : ''}
        </p>
      </div>

      {ordersQuery.isLoading ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading orders…</h2>
          <p className="empty-state-text">Fetching orders across all customers.</p>
        </div>
      ) : ordersQuery.isError ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load the orders</h2>
          <p className="empty-state-text">
            {describeApiError(
              ordersQuery.error,
              'The orders service is not responding right now.',
            )}
          </p>
          <div className="empty-state-actions">
            <button type="button" className="btn btn-primary" onClick={() => ordersQuery.refetch()}>
              Try again
            </button>
          </div>
        </div>
      ) : orders.length === 0 ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">No orders match</h2>
          <p className="empty-state-text">
            {status
              ? `No orders currently have the status ${ORDER_STATUS_LABELS[status]}.`
              : 'No orders have been placed yet.'}
          </p>
          <div className="empty-state-actions">
            {status && (
              <button type="button" className="btn btn-outline" onClick={() => changeStatus('')}>
                Clear filter
              </button>
            )}
          </div>
        </div>
      ) : (
        <>
          <ul className="order-list">
            {orders.map((order) => (
              <li key={order.id} className="order-row">
                <div className="order-row-main">
                  <p className="order-row-number">{order.orderNumber}</p>
                  <p className="cart-row-sku">#{order.id}</p>
                  {/* OrderResponse carries userId only — the admin DTO exposes no
                      customer name or email, so nothing else can be shown here. */}
                  <p className="cart-row-unit">Customer #{order.userId}</p>
                  <p className="cart-row-unit">Placed {formatDateTime(order.createdAt)}</p>
                </div>

                <div className="order-row-badges">
                  <OrderStatusBadge status={order.orderStatus} />
                  <PaymentStatusBadge status={order.paymentStatus} />
                </div>

                <div className="order-row-side">
                  <p className="order-row-total">{formatPrice(order.totalAmount)}</p>
                  <Link to={`/admin/orders/${order.id}`} className="btn btn-outline btn-sm">
                    Manage
                  </Link>
                </div>
              </li>
            ))}
          </ul>

          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </section>
  );
}

export default AdminOrdersPage;
