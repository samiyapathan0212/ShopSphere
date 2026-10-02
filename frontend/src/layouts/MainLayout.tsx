import { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Link, Outlet } from 'react-router-dom';
import ApiStatusBadge from '../components/ApiStatusBadge';
import NavBar from '../components/NavBar';
import { useRefreshMutation } from '../app/api/authApi';
import { credentialsReceived, loggedOut } from '../app/authSlice';

const FOOTER_COLUMNS = [
  {
    title: 'Shop',
    links: [
      { label: 'All products', to: '/products' },
      { label: 'Audio', to: '/products?category=audio' },
      { label: 'Wearables', to: '/products?category=wearables' },
      { label: 'Home', to: '/products?category=home' },
      { label: 'Workspace', to: '/products?category=workspace' },
      { label: 'Fitness', to: '/products?category=fitness' },
      { label: 'Outdoors', to: '/products?category=outdoors' },
    ],
  },
  {
    title: 'Account',
    links: [
      { label: 'Sign in', to: '/login' },
      { label: 'Sign up', to: '/register' },
      { label: 'Orders', to: '/orders' },
      { label: 'Wishlist', to: '/wishlist' },
      { label: 'Cart', to: '/cart' },
    ],
  },
  {
    title: 'Support',
    links: [
      { label: 'Shipping', to: '/products' },
      { label: 'Returns', to: '/products' },
      { label: 'Contact', to: '/products' },
      { label: 'Help', to: '/products' },
    ],
  },
];

/**
 * Shared application shell: the storefront navbar on top, the routed page in the
 * middle and a full footer (link columns, trust badges and the live API status
 * indicator) underneath.
 */
function MainLayout() {
  const dispatch = useDispatch();
  const [refresh] = useRefreshMutation();
  const bootstrapped = useRef(false);

  // Access tokens are short-lived, so a reloaded tab can hold one the backend
  // already rejects. The HttpOnly refresh cookie outlives the access token, so
  // exchange it for a fresh token once on boot instead of silently dropping the
  // session. A failed exchange means there is no valid session, so any stale
  // persisted state is cleared rather than left half-authenticated.
  useEffect(() => {
    if (bootstrapped.current) {
      return;
    }
    bootstrapped.current = true;

    void (async () => {
      try {
        const result = await refresh().unwrap();
        dispatch(credentialsReceived({ accessToken: result.accessToken, user: result.user }));
      } catch {
        dispatch(loggedOut());
      }
    })();
  }, [dispatch, refresh]);

  return (
    <div className="app-shell">
      <NavBar />
      <main className="app-main">
        <Outlet />
      </main>

      <footer className="app-footer">
        <div className="app-footer-grid">
          <div className="app-footer-brand">
            <span className="brand">
              <span className="brand-mark" aria-hidden="true">
                SS
              </span>
              <span className="brand-text">ShopSphere</span>
            </span>
            <p>
              A demonstration storefront for the ShopSphere commerce platform: curated products,
              clear pricing and straightforward delivery.
            </p>
            <p className="app-footer-trust">
              <span>Secure checkout</span>
              <span>Fast delivery</span>
              <span>30-day returns</span>
            </p>
          </div>

          {FOOTER_COLUMNS.map((column) => (
            <div key={column.title} className="app-footer-column">
              <h2>{column.title}</h2>
              <ul>
                {column.links.map((link) => (
                  <li key={link.label}>
                    <Link to={link.to}>{link.label}</Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="app-footer-inner">
          <p className="app-footer-copy">
            &copy; {new Date().getFullYear()} ShopSphere. Demonstration storefront, sample data only.
          </p>
          <ApiStatusBadge />
        </div>
      </footer>
    </div>
  );
}

export default MainLayout;