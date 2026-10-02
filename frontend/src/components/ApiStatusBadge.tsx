import { useGetHealthQuery } from '../app/api/apiSlice';

/**
 * Footer indicator that reuses the existing Phase 1 health endpoint. It never
 * blocks rendering and degrades to an "offline" label when the API is not
 * reachable, so the UI stays usable without a running backend.
 */
function ApiStatusBadge() {
  const { data, isLoading, isError } = useGetHealthQuery();

  const state = isLoading ? 'checking' : isError ? 'offline' : 'online';
  const details = data ? `${data.service}: ${data.status}` : undefined;

  return (
    <span className={`api-badge api-badge--${state}`} title={details}>
      <span className="api-badge-dot" aria-hidden="true" />
      API {state}
    </span>
  );
}

export default ApiStatusBadge;