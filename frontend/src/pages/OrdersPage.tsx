import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { describeApiError } from '../app/api/authApi';
import { useGetOrdersQuery } from '../app/api/orderApi';
import type { RootState } from '../app/store';
import { OrderStatusBadge, PaymentStatusBadge } from '../components/OrderStatusBadge';
import { formatDateTime } from '../components/OrderTimeline';
import Pagination from '../components/Pagination';
import { formatPrice } from '../mocks/catalog';

const PAGE_SIZE = 20;

/**
 * Customer orders page (Phase 5 client). Lists the signed-in customer's order
 * history straight from `GET /api/orders`, newest first.
 *
 * The backend pages are 0-based while the shared `Pagination` control is
 * 1-based, so the page index is kept 1-based here and converted on the way to
 * the API (`page - 1`).
 */
function OrdersPage() {
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const [page, setPage] = useState(1);

  const ordersQuery = useGetOrdersQuery({ page: page - 1, size: PAGE_SIZE }, { skip: !isAuthenticated });

  const orders = ordersQuery.data?.content ?? [];
  const totalPages = ordersQuery.data?.totalPages ?? 0;
  const totalElements = ordersQuery.data?.totalElements ?? 0;

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <span>Orders</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">Your orders</h1>
          <p className="page-lead">
            Every order you have placed, with its current status, payment result and total. Open an
            order to see its items, delivery address and tracking.
          </p>
        </div>
      </header>

      {!isAuthenticated ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Sign in to see your orders</h2>
          <p className="empty-state-text">
            Orders belong to your customer account, so sign in to see your order history.
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
      ) : ordersQuery.isLoading ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading your orders…</h2>
          <p className="empty-state-text">Fetching your order history.</p>
        </div>
      ) : ordersQuery.isError ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load your orders</h2>
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
            <Link to="/products" className="btn btn-outline">
              Browse products
            </Link>
          </div>
        </div>
      ) : orders.length === 0 ? (
        <div className="empty-state-card empty-state-card--centered">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <path
                d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinejoin="round"
              />
              <path
                d="M4 7.5l8 4.5 8-4.5M12 12v9"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <h2 className="empty-state-title">No orders yet</h2>
          <p className="empty-state-text">
            Once you place an order it appears here with its status, items and totals.
          </p>
          <div className="empty-state-actions">
            <Link to="/products" className="btn btn-primary">
              Browse products
            </Link>
            <Link to="/cart" className="btn btn-outline">
              View cart
            </Link>
          </div>
        </div>
      ) : (

        <>
          <p className="form-hint">
            {totalElements} {totalElements === 1 ? 'order' : 'orders'}
          </p>

          <ul className="order-list">
            {orders.map((order) => (
              <li key={order.id} className="order-row">
                <div className="order-row-main">
                  <p className="order-row-number">{order.orderNumber}</p>
                  <p className="cart-row-sku">#{order.id}</p>
                  <p className="cart-row-unit">Placed {formatDateTime(order.createdAt)}</p>
                </div>

                <div className="order-row-badges">
                  <OrderStatusBadge status={order.orderStatus} />
                  <PaymentStatusBadge status={order.paymentStatus} />
                </div>

                <div className="order-row-side">
                  <p className="order-row-total">{formatPrice(order.totalAmount)}</p>
                  <Link to={`/orders/${order.id}`} className="btn btn-outline btn-sm">
                    View order
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

export default OrdersPage;
