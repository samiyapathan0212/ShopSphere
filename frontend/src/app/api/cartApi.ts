import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

/** Mirrors the backend's `AddCartItemRequest` record. */
export interface AddCartItemRequest {
  productId: number;
  quantity: number;
}

/** Mirrors the backend's `UpdateCartItemRequest` record. */
export interface UpdateCartItemRequest {
  quantity: number;
}

/** Mirrors the backend's `CartItemResponse` record. */
export interface CartItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors the backend's `CartResponse` record. */
export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  subtotal: number;
  itemCount: number;
  itemCountWithQuantity: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Cart API slice (Phase 4A client). Extends the same RTK Query architecture as
 * `apiSlice` and `authApi` — no second cart state system. The current access
 * token from the existing auth slice is attached as the Bearer token the
 * backend's JWT filter expects, and `credentials: 'include'` keeps the refresh
 * cookie travelling like the auth calls. Every cart operation returns the full
 * cart, so a single 'Cart' tag keeps the navbar counter and the cart page in
 * step after every mutation.
 */
export const cartApi = createApi({
  reducerPath: 'cartApi',
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
  tagTypes: ['Cart'],
  endpoints: (builder) => ({
    getCart: builder.query<Cart, void>({
      query: () => '/cart',
      providesTags: ['Cart'],
    }),
    addItem: builder.mutation<Cart, AddCartItemRequest>({
      query: (body) => ({ url: '/cart/items', method: 'POST', body }),
      invalidatesTags: ['Cart'],
    }),
    updateItemQuantity: builder.mutation<Cart, { productId: number; body: UpdateCartItemRequest }>({
      query: ({ productId, body }) => ({
        url: `/cart/items/${productId}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['Cart'],
    }),
    removeItem: builder.mutation<Cart, number>({
      query: (productId) => ({ url: `/cart/items/${productId}`, method: 'DELETE' }),
      invalidatesTags: ['Cart'],
    }),
    clearCart: builder.mutation<Cart, void>({
      query: () => ({ url: '/cart', method: 'DELETE' }),
      invalidatesTags: ['Cart'],
    }),
  }),
});

export const {
  useGetCartQuery,
  useAddItemMutation,
  useUpdateItemQuantityMutation,
  useRemoveItemMutation,
  useClearCartMutation,
} = cartApi;