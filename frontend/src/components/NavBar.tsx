import { useEffect, useState, type FormEvent } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useLogoutMutation } from '../app/api/authApi';
import { useGetCartQuery } from '../app/api/cartApi';
import { useGetWishlistQuery } from '../app/api/wishlistApi';
import { loggedOut } from '../app/authSlice';
import type { RootState } from '../app/store';

const PRIMARY_LINKS = [
  { to: '/products', label: 'Shop all' },
  { to: '/products?category=audio', label: 'Audio' },
  { to: '/products?category=wearables', label: 'Wearables' },
  { to: '/products?category=home', label: 'Home' },
  { to: '/products?category=workspace', label: 'Workspace' },
  { to: '/products?category=fitness', label: 'Fitness' },
  { to: '/products?category=outdoors', label: 'Outdoors' },
];

function IconCart() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false">
      <path
        d="M3 4h2.2l2.2 10.4a2 2 0 0 0 2 1.6h7.3a2 2 0 0 0 2-1.5L20 7H6.2"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="10" cy="20" r="1.4" fill="currentColor" />
      <circle cx="17" cy="20" r="1.4" fill="currentColor" />
    </svg>
  );
}

function IconHeart() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false">
      <path
        d="M12 20s-7-4.3-7-9.3A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19 10.7c0 5-7 9.3-7 9.3Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function IconSearch() {
  return (
    <svg viewBox="0 0 24 24" width="17" height="17" aria-hidden="true" focusable="false">
      <circle cx="11" cy="11" r="6.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="m16 16 4 4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function IconSun() {
  return (
    <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="4.2" fill="none" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="M12 2.8v2M12 19.2v2M2.8 12h2M19.2 12h2M5.5 5.5l1.4 1.4M17.1 17.1l1.4 1.4M18.5 5.5l-1.4 1.4M6.9 17.1l-1.4 1.4"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
      />
    </svg>
  );
}

function IconMoon() {
  return (
    <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" focusable="false">
      <path
        d="M20 14.2A8.2 8.2 0 0 1 9.8 4 8.2 8.2 0 1 0 20 14.2Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
    </svg>
  );
}

type Theme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'shopsphere-theme';

/**
 * Reads the saved theme synchronously during the first render so the correct
 * palette is applied before paint. Light is the default and the OS colour
 * scheme is deliberately ignored, so the storefront never switches on its own.
 */
function readStoredTheme(): Theme {
  try {
    return window.localStorage.getItem(THEME_STORAGE_KEY) === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

/**
 * Storefront navbar: brand, catalogue search, primary navigation, wishlist and
 * cart counters, and the account actions (sign-in links when signed out, the
 * customer's name plus sign-out when signed in). The cart and wishlist counters
 * are live state from the cart and wishlist APIs — both hidden when signed out or
 * empty, so no mock count can survive a refresh. Below the mobile breakpoint
 * everything collapses behind the menu toggle.
 */
function NavBar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [theme, setTheme] = useState<Theme>(readStoredTheme);
  const { pathname, search } = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const user = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const [logout] = useLogoutMutation();
  // The cart badge reflects the real server cart; it is skipped entirely while
  // signed out so no fake count can ever show after a refresh.
  const cartQuery = useGetCartQuery(undefined, { skip: !isAuthenticated });
  const cartCount = cartQuery.data?.itemCountWithQuantity ?? 0;
  // Same treatment for the wishlist badge: real persisted count, never a mock.
  const wishlistQuery = useGetWishlistQuery(undefined, { skip: !isAuthenticated });
  const wishlistCount = wishlistQuery.data?.itemCount ?? 0;

  // Applies the theme to <html> (so the CSS token layer re-themes the whole app)
  // and mirrors it into localStorage so a reload restores the same palette.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Storage can be unavailable (private mode); the theme still applies.
    }
  }, [theme]);

  useEffect(() => {
    setMenuOpen(false);
    setAccountOpen(false);
  }, [pathname, search]);

  useEffect(() => {
    if (!accountOpen) {
      return undefined;
    }
    const close = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.closest('.nav-account')) {
        setAccountOpen(false);
      }
    };
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, [accountOpen]);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const term = searchTerm.trim();
    navigate(term ? `/products?q=${encodeURIComponent(term)}` : '/products');
  };

  const handleLogout = async () => {
    try {
      // Best effort: revokes the refresh-token cookie on the server.
      await logout().unwrap();
    } catch {
      // A failed server call must not keep the customer "signed in" locally.
    }
    dispatch(loggedOut());
    setMenuOpen(false);
    navigate('/');
  };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    isActive ? 'nav-link nav-link--active' : 'nav-link';

  return (
    <header className="navbar">
      <div className="navbar-topbar">
        <p>Free shipping on orders over $50 · Easy 30-day returns</p>
        <p className="navbar-topbar-links">
          <Link to="/products">New arrivals</Link>
          <Link to="/orders">Track an order</Link>
          <Link to="/register">Join ShopSphere</Link>
        </p>
      </div>

      <div className="navbar-inner">
        <Link to="/" className="brand" aria-label="ShopSphere home">
          <span className="brand-mark" aria-hidden="true">
            SS
          </span>
          <span className="brand-text">
            ShopSphere
            <span className="brand-tagline">Everyday commerce</span>
          </span>
        </Link>

        <form className="nav-search" role="search" onSubmit={handleSearch}>
          <label className="visually-hidden" htmlFor="navbar-search">
            Search products
          </label>
          <span className="nav-search-icon">
            <IconSearch />
          </span>
          <input
            id="navbar-search"
            className="nav-search-input"
            type="search"
            placeholder="Search headphones, desks, kitchen..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
          <button type="submit" className="btn btn-primary btn-sm nav-search-submit">
            Search
          </button>
        </form>

        <div className="navbar-actions">
          <div className="theme-toggle" role="group" aria-label="Colour theme">
            <button
              type="button"
              className={
                theme === 'light' ? 'theme-toggle-btn theme-toggle-btn--active' : 'theme-toggle-btn'
              }
              onClick={() => setTheme('light')}
              aria-pressed={theme === 'light'}
              aria-label="Switch to light theme"
              title="Light theme"
            >
              <IconSun />
            </button>
            <button
              type="button"
              className={
                theme === 'dark' ? 'theme-toggle-btn theme-toggle-btn--active' : 'theme-toggle-btn'
              }
              onClick={() => setTheme('dark')}
              aria-pressed={theme === 'dark'}
              aria-label="Switch to dark theme"
              title="Dark theme"
            >
              <IconMoon />
            </button>
          </div>

          <Link
            to="/wishlist"
            className="nav-icon-btn"
            aria-label={wishlistCount > 0 ? `Wishlist, ${wishlistCount} items` : 'Wishlist'}
          >
            <IconHeart />
            {wishlistCount > 0 && <span className="nav-badge">{wishlistCount}</span>}
          </Link>

          <Link
            to="/cart"
            className="nav-icon-btn"
            aria-label={cartCount > 0 ? `Cart, ${cartCount} items` : 'Cart'}
          >
            <IconCart />
            {cartCount > 0 && <span className="nav-badge">{cartCount}</span>}
          </Link>

          {isAuthenticated && user ? (
            <div className="nav-account">
              <button
                type="button"
                className="account-chip"
                aria-haspopup="menu"
                aria-expanded={accountOpen}
                onClick={() => setAccountOpen((open) => !open)}
              >
                <span className="account-chip-avatar" aria-hidden="true">
                  {user.name.charAt(0).toUpperCase()}
                </span>
                <span className="account-chip-name">{user.name.split(' ')[0]}</span>
              </button>

              {accountOpen && (
                <div className="account-menu" role="menu">
                  <p className="account-menu-header">Signed in as {user.email}</p>
                  <Link role="menuitem" to="/orders" onClick={() => setAccountOpen(false)}>
                    My orders
                  </Link>
                  {user.role === 'ADMIN' && (
                    <Link role="menuitem" to="/admin/orders" onClick={() => setAccountOpen(false)}>
                      Admin orders
                    </Link>
                  )}
                  <Link role="menuitem" to="/wishlist" onClick={() => setAccountOpen(false)}>
                    Wishlist
                  </Link>
                  <Link role="menuitem" to="/cart" onClick={() => setAccountOpen(false)}>
                    Cart
                  </Link>
                  <button type="button" role="menuitem" className="account-menu-logout" onClick={handleLogout}>
                    Log out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="nav-auth">
              <Link to="/login" className="nav-link">
                Sign in
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">
                Sign up
              </Link>
            </div>
          )}

          <button
            type="button"
            className="nav-toggle"
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span aria-hidden="true" />
            <span aria-hidden="true" />
            <span aria-hidden="true" />
          </button>
        </div>
      </div>

      <nav className="navbar-nav" aria-label="Primary">
        <ul>
          {PRIMARY_LINKS.map((link) => {
            const isCategoryLink = link.to.includes('?category=');
            const active = isCategoryLink
              ? pathname === '/products' && search.includes(link.to.split('?')[1])
              : undefined;
            return (
              <li key={link.label}>
                {isCategoryLink ? (
                  <Link
                    to={link.to}
                    className={active ? 'nav-link nav-link--active' : 'nav-link'}
                  >
                    {link.label}
                  </Link>
                ) : (
                  <NavLink to={link.to} className={linkClass} end={link.to === '/products'}>
                    {link.label}
                  </NavLink>
                )}
              </li>
            );
          })}
        </ul>
      </nav>

      {menuOpen && (
        <div className="mobile-menu" id="mobile-menu">
          <ul className="mobile-menu-links">
            {PRIMARY_LINKS.map((link) => (
              <li key={link.label}>
                <Link to={link.to}>{link.label}</Link>
              </li>
            ))}
            <li>
              <Link to="/orders">Orders</Link>
            </li>
            <li>
              <Link to="/wishlist">
                Wishlist
                {wishlistCount > 0 && (
                  <span className="nav-badge nav-badge--inline">{wishlistCount}</span>
                )}
              </Link>
            </li>
            <li>
              <Link to="/cart">
                Cart
                {cartCount > 0 && (
                  <span className="nav-badge nav-badge--inline">{cartCount}</span>
                )}
              </Link>
            </li>
          </ul>

          <div className="mobile-menu-actions">
            {isAuthenticated && user ? (
              <>
                <p className="mobile-menu-user">
                  Signed in as <strong>{user.name}</strong>
                </p>
                <button type="button" className="btn btn-outline btn-block" onClick={handleLogout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-outline btn-block">
                  Sign in
                </Link>
                <Link to="/register" className="btn btn-primary btn-block">
                  Sign up
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

export default NavBar;
