import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import { useGetWishlistQuery } from '../app/api/wishlistApi';
import { describeApiError } from '../app/api/authApi';
import type { RootState } from '../app/store';
import { findMockProductById, getBestSellerProducts, type MockProduct } from '../mocks/catalog';

/**
 * Wishlist page. Reads the customer's persisted wishlist from the backend and
 * renders the matching catalogue products, so a heart pressed on any product
 * card or detail page shows up here and survives a reload. Signed-out visitors
 * are offered sign in, an empty wishlist keeps the best-seller suggestions, and
 * nothing on this page is mock wishlist data.
 */
function WishlistPage() {
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const wishlistQuery = useGetWishlistQuery(undefined, { skip: !isAuthenticated });
  const suggestions = getBestSellerProducts(4);

  // The API stores product ids; the catalogue supplies the presentation data.
  const savedProducts: MockProduct[] = (wishlistQuery.data?.items ?? [])
    .map((item) => findMockProductById(item.productId))
    .filter((product): product is MockProduct => product !== undefined);

  const savedIds = new Set(savedProducts.map((product) => product.id));
  const suggestedProducts = suggestions.filter((product) => !savedIds.has(product.id));
  const loadError = wishlistQuery.isError
    ? describeApiError(wishlistQuery.error, 'We could not load your wishlist. Please try again.')
    : null;

  const isLoading = wishlistQuery.isLoading || wishlistQuery.isFetching;

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <span>Wishlist</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">Wishlist</h1>
          <p className="page-lead">
            {isAuthenticated
              ? 'Everything you have saved, ready to review whenever you return.'
              : 'Sign in to see the products you have saved to your wishlist.'}
          </p>
        </div>
      </header>

      {loadError && <p className="form-alert">{loadError}</p>}

      {!isAuthenticated ? (
        <div className="empty-state-card empty-state-card--centered">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <path
                d="M12 20s-7-4.3-7-9.3A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19 10.7c0 5-7 9.3-7 9.3Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <h2 className="empty-state-title">Sign in to your wishlist</h2>
          <p className="empty-state-text">
            Your wishlist is tied to your account, so sign in to see the products you have saved on
            any device.
          </p>
          <div className="empty-state-actions">
            <Link to="/login" className="btn btn-primary">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-outline">
              Create an account
            </Link>
          </div>
        </div>
      ) : isLoading ? (
        <p className="demo-note">Loading your saved products…</p>
      ) : savedProducts.length === 0 ? (
        <div className="empty-state-card empty-state-card--centered">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <path
                d="M12 20s-7-4.3-7-9.3A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19 10.7c0 5-7 9.3-7 9.3Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <h2 className="empty-state-title">Your wishlist is empty</h2>
          <p className="empty-state-text">
            Tap the heart on any product to keep it for later. Start with the customer favourites
            below, or browse the full catalogue.
          </p>
          <div className="empty-state-actions">
            <Link to="/products" className="btn btn-primary">
              Browse products
            </Link>
          </div>
        </div>
      ) : (
        <ul className="product-grid product-grid--four">
          {savedProducts.map((product) => (
            <li key={product.id}>
              <ProductCard product={product} />
            </li>
          ))}
        </ul>
      )}

      {isAuthenticated && suggestedProducts.length > 0 && (
        <section className="section" aria-labelledby="wishlist-suggestions-title">
          <header className="section-head">
            <div>
              <p className="eyebrow">Customer favourites</p>
              <h2 id="wishlist-suggestions-title" className="section-title">
                Best sellers
              </h2>
            </div>
            <Link to="/products?sort=rating" className="link-arrow">
              Top rated
            </Link>
          </header>
          <ul className="product-grid product-grid--four">
            {suggestedProducts.slice(0, 4).map((product) => (
              <li key={product.id}>
                <ProductCard product={product} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
}

export default WishlistPage;
