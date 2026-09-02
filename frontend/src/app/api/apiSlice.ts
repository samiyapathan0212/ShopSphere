import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

/**
 * Base RTK Query API slice. Business-focused endpoints will be added
 * as this API slice is extended in later phases.
 */
export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl: API_URL }),
  endpoints: (builder) => ({
    getHealth: builder.query<HealthResponse, void>({
      query: () => '/health',
    }),
  }),
});

export const { useGetHealthQuery } = apiSlice;