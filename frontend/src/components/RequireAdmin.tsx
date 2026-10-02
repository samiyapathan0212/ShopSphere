import type { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import type { RootState } from '../app/store';

interface RequireAdminProps {
  children: ReactNode;
}

/**
 * Route guard for the admin area.
 *
 * This is a client-side convenience only: it reads the role already present in
 * the Redux auth slice and refuses to render the admin UI for anyone who is not
 * an ADMIN, so a customer typing `/admin/orders` gets an explanation instead of a
 * broken screen. It introduces no second authentication mechanism — the token
 * still lives in the auth slice, and the real enforcement is the backend's
 * `@PreAuthorize("hasRole('ADMIN')")` on every `/api/admin/**` route, which
 * answers 403 regardless of what the browser renders.
 */
export function RequireAdmin({ children }: RequireAdminProps) {
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const role = useSelector((state: RootState) => state.auth.user?.role);

  if (!isAuthenticated) {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Sign in to continue</h2>
          <p className="empty-state-text">
            The administration area is only available to signed-in staff accounts.
          </p>
          <div className="empty-state-actions">
            <Link to="/login" className="btn btn-primary">
              Sign in
            </Link>
            <Link to="/products" className="btn btn-outline">
              Browse products
            </Link>
          </div>
        </div>
      </section>
    );
  }

  if (role !== 'ADMIN') {
    return (
      <section className="page-section">
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Administrator access required</h2>
          <p className="empty-state-text">
            This account does not have the administrator role, so the order management tools are not
            available. If you believe this is wrong, ask an administrator to review your account.
          </p>
          <div className="empty-state-actions">
            <Link to="/orders" className="btn btn-primary">
              Go to my orders
            </Link>
            <Link to="/products" className="btn btn-outline">
              Browse products
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return <>{children}</>;
}

export default RequireAdmin;
