import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';
import type { PaymentStatus } from './orderApi';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

/** Mirrors the backend `PaymentResponse` record. */
export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  paymentStatus: PaymentStatus;
  provider: string;
  providerPaymentId: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors the backend `PaymentVerificationResponse` record. */
export interface PaymentVerification {
  paymentId: number;
  orderId: number;
  verified: boolean;
  paymentStatus: PaymentStatus;
  message: string;
}

/**
 * Payment API slice (Phase 5 client). Same RTK Query architecture and the same
 * Bearer/cookie conventions as the other authenticated slices.
 *
 * IMPORTANT: the `{id}` path variable on every route below is the ORDER id, not
 * the payment id — the backend keys payments by order (exactly one payment per
 * order), which is why the argument type is a plain number used as the order id.
 */
export const paymentApi = createApi({
  reducerPath: 'paymentApi',
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
  tagTypes: ['Payment'],
  endpoints: (builder) => ({
    getPayment: builder.query<Payment, number>({
      query: (orderId) => `/payments/${orderId}`,
      providesTags: (_result, _error, orderId) => [{ type: 'Payment' as const, id: orderId }],
    }),
    /**
     * Retries a payment through the sandbox gateway. The backend's body is
     * optional and is left off on purpose: the sandbox `simulateFailure` hook is
     * a test switch, not something the storefront should send.
     */
    processPayment: builder.mutation<Payment, number>({
      query: (orderId) => ({ url: `/payments/${orderId}/process`, method: 'POST' }),
      invalidatesTags: (_result, _error, orderId) => [{ type: 'Payment' as const, id: orderId }],
    }),
    verifyPayment: builder.mutation<PaymentVerification, number>({
      query: (orderId) => ({ url: `/payments/${orderId}/verify`, method: 'POST' }),
    }),
  }),
});

export const { useGetPaymentQuery, useProcessPaymentMutation, useVerifyPaymentMutation } =
  paymentApi;
