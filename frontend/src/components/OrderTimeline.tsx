import type { OrderStatus, OrderTracking } from '../app/api/orderApi';
import { ORDER_STATUS_LABELS, OrderStatusBadge, PaymentStatusBadge } from './OrderStatusBadge';

/** Shared date/time formatting for order surfaces (ISO instants from the API). */
export function formatDateTime(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  const date = parsed.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
  const time = parsed.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  return `${date} at ${time}`;
}

/**
 * The order lifecycle in the order the backend's state machine advances through
 * it (`OrderStatus.canTransitionTo`). `CANCELLED` is deliberately absent: it is
 * a terminal branch off this path, not a step on it.
 */
const LIFECYCLE: OrderStatus[] = [
  'PLACED',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

interface OrderTimelineProps {
  /** The backend `OrderTrackingResponse` for the order. */
  tracking: OrderTracking;
}

/**
 * Order tracking strip.
 *
 * The backend tracks a single current position — it exposes no per-stage event
 * history, so this renders the lifecycle with the current step highlighted and
 * marks the surrounding steps as completed or upcoming purely from that status.
 * No scan timestamps, carrier events or history rows are invented; the only
 * real timestamps shown are the ones the API returns.
 */
export function OrderTimeline({ tracking }: OrderTimelineProps) {
  const cancelled = tracking.orderStatus === 'CANCELLED';
  const currentIndex = LIFECYCLE.indexOf(tracking.orderStatus);

  return (
    <div className="order-timeline">
      <header className="order-timeline-head">
        <div>
          <p className="eyebrow">Tracking</p>
          <h2 className="section-title">Delivery progress</h2>
        </div>
        <div className="order-timeline-badges">
          <OrderStatusBadge status={tracking.orderStatus} />
          <PaymentStatusBadge status={tracking.paymentStatus} />
        </div>
      </header>

      {cancelled ? (
        <p className="form-alert" role="status">
          This order was cancelled and is no longer moving through the delivery steps.
        </p>
      ) : (
        <ol className="steps-strip steps-strip--order">
          {LIFECYCLE.map((step, index) => {
            const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'upcoming';
            return (
              <li
                key={step}
                className={`step-card step-card--${state}`}
                aria-current={state === 'current' ? 'step' : undefined}
              >
                <span className={`step-number step-number--${state}`} aria-hidden="true">
                  {state === 'done' ? '✓' : index + 1}
                </span>
                <h3 className="step-title">{ORDER_STATUS_LABELS[step]}</h3>
              </li>
            );
          })}
        </ol>
      )}

      <dl className="order-meta">
        <div>
          <dt>Order number</dt>
          <dd>{tracking.orderNumber}</dd>
        </div>
        <div>
          <dt>Placed</dt>
          <dd>{formatDateTime(tracking.createdAt)}</dd>
        </div>
        <div>
          <dt>Last updated</dt>
          <dd>{formatDateTime(tracking.updatedAt)}</dd>
        </div>
        <div>
          <dt>Can still be cancelled</dt>
          <dd>{tracking.cancellable ? 'Yes' : 'No'}</dd>
        </div>
      </dl>

      <p className="form-hint">
        Tracking shows the current stage of your order. ShopSphere does not record carrier scan
        history, so no past events are shown.
      </p>
    </div>
  );
}

export default OrderTimeline;
