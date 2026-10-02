import { configureStore } from '@reduxjs/toolkit';
import { apiSlice } from './api/apiSlice';
import { adminOrderApi } from './api/adminOrderApi';
import { authApi } from './api/authApi';
import { cartApi } from './api/cartApi';
import { checkoutApi } from './api/checkoutApi';
import { orderApi } from './api/orderApi';
import { paymentApi } from './api/paymentApi';
import { wishlistApi } from './api/wishlistApi';
import authReducer, { clearPersistedAuthState, persistAuthState, type AuthState } from './authSlice';

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    [adminOrderApi.reducerPath]: adminOrderApi.reducer,
    [authApi.reducerPath]: authApi.reducer,
    [cartApi.reducerPath]: cartApi.reducer,
    [checkoutApi.reducerPath]: checkoutApi.reducer,
    [orderApi.reducerPath]: orderApi.reducer,
    [paymentApi.reducerPath]: paymentApi.reducer,
    [wishlistApi.reducerPath]: wishlistApi.reducer,
    auth: authReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      apiSlice.middleware,
      adminOrderApi.middleware,
      authApi.middleware,
      cartApi.middleware,
      checkoutApi.middleware,
      orderApi.middleware,
      paymentApi.middleware,
      wishlistApi.middleware,
    ),
});

// Keep the persisted session in step with the store without adding middleware.
store.subscribe(() => {
  const auth: AuthState = store.getState().auth;
  if (auth.accessToken && auth.user) {
    persistAuthState(auth);
  } else {
    clearPersistedAuthState();
  }
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;