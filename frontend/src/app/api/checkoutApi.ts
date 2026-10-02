import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';
import type { Order } from './orderApi';
import type { Payment } from './paymentApi';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

/** Mirrors the backend `CheckoutRequest` record (the shipping address snapshot). */
export interface CheckoutRequest {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  simulatePaymentFailure: boolean;
}

/**
 * Mirrors the backend `CheckoutResponse` record: the created order together
 * with the payment the checkout transaction already charged through the sandbox
 * gateway. A declined charge is still returned as 201 with the payment FAILED
 * and the order CANCELLED, so the confirmation page branches on
 * `payment.paymentStatus` rather than assuming success.
 */
export interface CheckoutResponse {
  order: Order;
  payment: Payment;
}

/**
 * Checkout API slice (Phase 5 client). One mutation, because the backend folds
 * the whole purchase into a single transaction: cart validation, stock
 * reservation, order creation, the sandbox charge and cart clearing all happen
 * server-side inside `POST /api/checkout`. There is deliberately no separate
 * card/payment-gateway form on the client.
 *
 * Invalidating `Cart` clears the cart page and the navbar counter in one step
 * (the backend empties the cart only when the order was created and paid), and
 * the order history tag makes the new order appear without a reload.
 */
export const checkoutApi = createApi({
  reducerPath: 'checkoutApi',
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
  // Declared even though this slice only has mutations: it invalidates the
  // `Cart` tag owned by cartApi and the order-history tag owned by orderApi, and
  // RTK Query needs the tag types known on the slice that names them.
  tagTypes: ['Cart', 'Order'],
  endpoints: (builder) => ({
    checkout: builder.mutation<CheckoutResponse, CheckoutRequest>({
      query: (body) => ({ url: '/checkout', method: 'POST', body }),
      invalidatesTags: ['Cart', { type: 'Order', id: 'LIST' }],
    }),
  }),
});

export const { useCheckoutMutation } = checkoutApi;
