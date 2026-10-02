import type { OrderStatus, PaymentStatus } from '../app/api/orderApi';

type BadgeTone = 'info' | 'progress' | 'success' | 'danger' | 'muted';

/**
 * Readable labels for the backend `OrderStatus` enum. Every status the backend
 * can return is covered; nothing is invented.
 */
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PLACED: 'Placed',
  CONFIRMED: 'Confirmed',
  PROCESSING: 'Processing',
  SHIPPED: 'Shipped',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

const ORDER_STATUS_TONES: Record<OrderStatus, BadgeTone> = {
  PLACED: 'info',
  CONFIRMED: 'info',
  PROCESSING: 'progress',
  SHIPPED: 'progress',
  OUT_FOR_DELIVERY: 'progress',
  DELIVERED: 'success',
  CANCELLED: 'muted',
};

/** Readable labels for the backend `PaymentStatus` enum. */
const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Payment pending',
  PAID: 'Paid',
  FAILED: 'Payment failed',
  REFUNDED: 'Refunded',
};

const PAYMENT_STATUS_TONES: Record<PaymentStatus, BadgeTone> = {
  PENDING: 'progress',
  PAID: 'success',
  FAILED: 'danger',
  REFUNDED: 'info',
};

interface OrderStatusBadgeProps {
  status: OrderStatus;
}

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
}

/** Small pill showing the current order lifecycle status. */
export function OrderStatusBadge({ status }: OrderStatusBadgeProps) {
  return (
    <span className={`status-badge status-badge--${ORDER_STATUS_TONES[status]}`}>
      {ORDER_STATUS_LABELS[status]}
    </span>
  );
}

/** Small pill showing the mirrored payment status carried on the order. */
export function PaymentStatusBadge({ status }: PaymentStatusBadgeProps) {
  return (
    <span className={`status-badge status-badge--${PAYMENT_STATUS_TONES[status]}`}>
      {PAYMENT_STATUS_LABELS[status]}
    </span>
  );
}

export default OrderStatusBadge;
