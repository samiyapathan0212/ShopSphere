import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthState } from '../authSlice';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

/** Mirrors the backend's `WishlistItemResponse` record. */
export interface WishlistItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors the backend's `WishlistResponse` record. */
export interface Wishlist {
  id: number;
  userId: number;
  items: WishlistItem[];
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Wishlist API slice. Extends the same RTK Query architecture as `apiSlice`,
 * `authApi` and `cartApi` — no second wishlist state system. The access token
 * from the existing auth slice is attached as the Bearer token the backend's
 * JWT filter expects, and `credentials: 'include'` keeps the refresh cookie
 * travelling like the other authenticated calls. Every wishlist operation
 * returns the full wishlist, so a single 'Wishlist' tag keeps the navbar
 * counter, the heart buttons and the wishlist page in step after every
 * mutation.
 */
export const wishlistApi = createApi({
  reducerPath: 'wishlistApi',
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
  tagTypes: ['Wishlist'],
  endpoints: (builder) => ({
    getWishlist: builder.query<Wishlist, void>({
      query: () => '/wishlist',
      providesTags: ['Wishlist'],
    }),
    addItem: builder.mutation<Wishlist, number>({
      query: (productId) => ({ url: `/wishlist/items/${productId}`, method: 'POST' }),
      invalidatesTags: ['Wishlist'],
    }),
    removeItem: builder.mutation<Wishlist, number>({
      query: (productId) => ({ url: `/wishlist/items/${productId}`, method: 'DELETE' }),
      invalidatesTags: ['Wishlist'],
    }),
  }),
});

// RTK Query derives hook names from the endpoint keys, so the mutation hooks are
// aliased here to the wishlist-specific names the components import.
export const {
  useGetWishlistQuery,
  useAddItemMutation: useAddWishlistItemMutation,
  useRemoveItemMutation: useRemoveWishlistItemMutation,
} = wishlistApi;