import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';
import type { Order, OrderStatus, PagedResult } from './orderApi';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

export interface AdminOrderListArgs {
  /** 0-based page index, matching the backend's `PageResponse`. */
  page: number;
  size: number;
  /** Optional filter; omitted entirely when null so the backend lists all. */
  status?: OrderStatus | null;
}

export interface UpdateAdminOrderStatusArgs {
  id: number;
  orderStatus: OrderStatus;
}

/**
 * Admin order API slice. Wraps the three ADMIN-only routes that already exist on
 * the backend (`GET /api/admin/orders`, `GET /api/admin/orders/{id}` and
 * `PUT /api/admin/orders/{id}/status`).
 *
 * Requests reuse the same `OrderResponse` and `PageResponse` DTOs as the
 * customer slice, so the shared types are imported rather than duplicated.
 * A separate `AdminOrder` tag space is used deliberately: admin data is not the
 * customer's own order cache, and the two must not invalidate each other.
 *
 * Authorization is enforced by the backend's `@PreAuthorize("hasRole('ADMIN')")`;
 * this slice only needs to send the admin's own Bearer token.
 */
export const adminOrderApi = createApi({
  reducerPath: 'adminOrderApi',
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
  tagTypes: ['AdminOrder'],
  endpoints: (builder) => ({
    getAdminOrders: builder.query<PagedResult<Order>, AdminOrderListArgs>({
      query: ({ page, size, status }) => {
        const params = new URLSearchParams({ page: String(page), size: String(size) });
        if (status) {
          params.set('status', status);
        }
        return { url: `/admin/orders?${params.toString()}` };
      },
      providesTags: (result) =>
        result
          ? [
              { type: 'AdminOrder' as const, id: 'LIST' },
              ...result.content.map((order) => ({ type: 'AdminOrder' as const, id: order.id })),
            ]
          : [{ type: 'AdminOrder' as const, id: 'LIST' }],
    }),
    getAdminOrder: builder.query<Order, number>({
      query: (id) => `/admin/orders/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'AdminOrder' as const, id }],
    }),
    updateAdminOrderStatus: builder.mutation<Order, UpdateAdminOrderStatusArgs>({
      query: ({ id, orderStatus }) => ({
        url: `/admin/orders/${id}/status`,
        method: 'PUT',
        body: { orderStatus },
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'AdminOrder' as const, id },
        { type: 'AdminOrder' as const, id: 'LIST' },
      ],
    }),
  }),
});

export const { useGetAdminOrdersQuery, useGetAdminOrderQuery, useUpdateAdminOrderStatusMutation } =
  adminOrderApi;
