import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

/** Mirrors the backend `OrderStatus` enum. */
export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

/** Mirrors the backend `PaymentStatus` enum. */
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

/** Mirrors the backend `OrderItemResponse` record (immutable purchase snapshot). */
export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  priceAtPurchase: number;
  quantity: number;
  lineTotal: number;
}

/** Mirrors the backend `OrderResponse` record. */
export interface Order {
  id: number;
  userId: number;
  orderNumber: string;
  orderStatus: OrderStatus;
  paymentStatus: PaymentStatus;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  shippingFee: number;
  totalAmount: number;
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Mirrors the backend `OrderTrackingResponse` record.
 *
 * Note it carries no order `id` and no event history: the backend exposes only
 * the current lifecycle position, the mirrored payment status and whether the
 * order can still be cancelled. The tracking UI must not invent the missing
 * pieces (see `OrderTimeline`).
 */
export interface OrderTracking {
  orderNumber: string;
  orderStatus: OrderStatus;
  paymentStatus: PaymentStatus;
  cancellable: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors the backend `PageResponse<T>` record. `page` is 0-based. */
export interface PagedResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Customer order API slice (Phase 5 client). Extends the same RTK Query
 * architecture as `apiSlice`, `authApi`, `cartApi` and `wishlistApi` — no
 * second order state system. The access token from the existing auth slice is
 * attached as the Bearer token the backend's JWT filter expects, and
 * `credentials: 'include'` keeps the refresh cookie travelling like the other
 * authenticated calls.
 *
 * Every order read is tagged with the order id and the list tag, so a
 * cancellation (or a checkout elsewhere in the app) refreshes exactly the
 * affected detail, tracking and history entries.
 */
export const orderApi = createApi({
  reducerPath: 'orderApi',
  baseQuery: fetchBaseQuery({
    baseUrl: API_URL,
    credentials: 'include',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as { auth?: AuthState }).auth?.accessToken;
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Order'],
  endpoints: (builder) => ({
    getOrders: builder.query<PagedResult<Order>, { page: number; size: number }>({
      query: ({ page, size }) => ({ url: `/orders?page=${page}&size=${size}` }),
      providesTags: (result) =>
        result
          ? [
              { type: 'Order' as const, id: 'LIST' },
              ...result.content.map((order) => ({ type: 'Order' as const, id: order.id })),
            ]
          : [{ type: 'Order' as const, id: 'LIST' }],
    }),
    getOrder: builder.query<Order, number>({
      query: (id) => `/orders/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Order' as const, id }],
    }),
    getOrderTracking: builder.query<OrderTracking, number>({
      query: (id) => `/orders/${id}/tracking`,
      providesTags: (_result, _error, id) => [{ type: 'Order' as const, id }],
    }),
    cancelOrder: builder.mutation<Order, number>({
      query: (id) => ({ url: `/orders/${id}/cancel`, method: 'POST' }),
      invalidatesTags: (_result, _error, id) => [
        { type: 'Order' as const, id },
        { type: 'Order' as const, id: 'LIST' },
      ],
    }),
  }),
});

export const { useGetOrdersQuery, useGetOrderQuery, useGetOrderTrackingQuery, useCancelOrderMutation } =
  orderApi;
