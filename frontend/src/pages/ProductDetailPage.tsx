import { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link, useParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import RatingStars from '../components/RatingStars';
import { useAddItemMutation } from '../app/api/cartApi';
import {
  useAddWishlistItemMutation,
  useGetWishlistQuery,
  useRemoveWishlistItemMutation,
} from '../app/api/wishlistApi';
import { describeApiError } from '../app/api/authApi';
import type { RootState } from '../app/store';
import {
  discountPercent,
  findCategoryById,
  findMockProductById,
  findMockProductBySlug,
  formatPrice,
  getProductsByCategory,
} from '../mocks/catalog';

const DELIVERY_POINTS = [
  {
    title: 'Free delivery over $50',
    copy: 'Standard delivery in 2-4 working days, dispatched within one working day.',
    icon: 'truck' as const,
  },
  {
    title: '30-day free returns',
    copy: 'Changed your mind? Start a return from your account and send it back free.',
    icon: 'return' as const,
  },
  {
    title: '2-year warranty',
    copy: 'Every electronic item includes the full manufacturer warranty as standard.',
    icon: 'shield' as const,
  },
  {
    title: 'Secure payments',
    copy: 'Checkout runs over an encrypted connection; card details stay with a tokenised provider.',
    icon: 'lock' as const,
  },
];

function DetailIcon({ kind }: { kind: 'truck' | 'return' | 'shield' | 'lock' }) {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      {kind === 'truck' && (
        <>
          <path
            d="M2.5 6.5H13v8H2.5z M13 9.5h3.6l2.9 3v2H13z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
          <circle cx="6.2" cy="16.6" r="1.6" fill="none" stroke="currentColor" strokeWidth="1.4" />
          <circle cx="16.2" cy="16.6" r="1.6" fill="none" stroke="currentColor" strokeWidth="1.4" />
        </>
      )}
      {kind === 'return' && (
        <>
          <path
            d="M20 12a8 8 0 1 1-2.34-5.66"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
          />
          <path
            d="M20 4v4.2h-4.2"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
      {kind === 'shield' && (
        <>
          <path
            d="M12 3l7 3v5c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
          <path
            d="m9 11.5 2 2 4-4"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
      {kind === 'lock' && (
        <>
          <rect
            x="5.5"
            y="10.5"
            width="13"
            height="9"
            rx="2"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
          />
          <path
            d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
          />
        </>
      )}
    </svg>
  );
}

/**
 * Product detail. The product, its gallery and the related items come from the
 * isolated mock catalog; quantity and wishlist remain local demonstration
 * state. Add to cart goes through the real cart API — a customer account is
 * required, so signed-out visitors get a sign-in call to action instead.
 */
function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();

  const product = useMemo(() => {
    const numericId = Number(id);
    if (Number.isInteger(numericId) && numericId > 0) {
      return findMockProductById(numericId) ?? findMockProductBySlug(id ?? '');
    }
    return findMockProductBySlug(id ?? '');
  }, [id]);

  const [galleryIndex, setGalleryIndex] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const [addItem, { isLoading: adding }] = useAddItemMutation();
  // The wishlist state is read from the server so the button survives a reload
  // and matches the navbar counter. It is skipped for signed-out visitors.
  const wishlistQuery = useGetWishlistQuery(undefined, { skip: !isAuthenticated });
  const [addWishlistItem, { isLoading: wishlistAdding }] = useAddWishlistItemMutation();
  const [removeWishlistItem, { isLoading: wishlistRemoving }] = useRemoveWishlistItemMutation();
  const saved =
    product !== undefined &&
    (wishlistQuery.data?.items.some((item) => item.productId === product.id) ?? false);
  const wishlistBusy = wishlistAdding || wishlistRemoving;

  useEffect(() => {
    setGalleryIndex(0);
    setQuantity(1);
    setAdded(false);
    setActionError(null);
  }, [product?.id]);

  // The heart toggles the persisted wishlist; the label only changes once the
  // API call has actually succeeded.
  const handleToggleWishlist = async () => {
    if (wishlistBusy || !product) {
      return;
    }
    setActionError(null);
    try {
      if (saved) {
        await removeWishlistItem(product.id).unwrap();
      } else {
        await addWishlistItem(product.id).unwrap();
      }
    } catch (error) {
      setActionError(describeApiError(error, 'Could not update your wishlist. Please try again.'));
    }
  };

  // Guarded against double clicks: the button is disabled while the mutation is
  // in flight, so the in-flight check is a second line of defence.
  const handleAddToCart = async () => {
    if (adding || !product || !product.inStock) {
      return;
    }
    setActionError(null);
    try {
      await addItem({ productId: product.id, quantity }).unwrap();
      setAdded(true);
    } catch (error) {
      setActionError(describeApiError(error, 'Could not add this product to the cart.'));
    }
  };

  useEffect(() => {
    if (!added) {
      return undefined;
    }
    const timer = window.setTimeout(() => setAdded(false), 1500);
    return () => window.clearTimeout(timer);
  }, [added]);

  if (!product) {
    return (
      <section className="page-section">
        <div className="empty-state-card">
          <span className="empty-state-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="26" height="26" focusable="false">
              <circle cx="11" cy="11" r="6.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
              <path
                d="m16 16 4 4"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </span>
          <h1 className="empty-state-title">Product not found</h1>
          <p className="empty-state-text">
            The product you are looking for is not part of the demo catalogue. Browse the full
            range instead.
          </p>
          <div className="empty-state-actions">
            <Link to="/products" className="btn btn-primary">
              Browse all products
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const category = findCategoryById(product.categoryId);
  const discount = discountPercent(product);
  const activeImage = product.images[Math.min(galleryIndex, product.images.length - 1)];
  const related = getProductsByCategory(product.categoryId, 5)
    .filter((candidate) => candidate.id !== product.id)
    .slice(0, 4);

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link to="/products">All products</Link>
        {category && (
          <>
            <span aria-hidden="true">/</span>
            <Link to={`/products?category=${category.id}`}>{category.name}</Link>
          </>
        )}
        <span aria-hidden="true">/</span>
        <span>{product.name}</span>
      </nav>

      <div className="detail-layout">
        <div className="detail-gallery">
          <div className="detail-gallery-main">
            <img src={activeImage} alt={product.name} />
            {product.badge && (
              <span className="product-badge product-badge--main">{product.badge}</span>
            )}
          </div>
          {product.images.length > 1 && (
            <div className="detail-thumbs" role="group" aria-label="Product images">
              {product.images.map((image, index) => (
                <button
                  key={image}
                  type="button"
                  className={
                    index === galleryIndex ? 'detail-thumb detail-thumb--active' : 'detail-thumb'
                  }
                  aria-label={`Show image ${index + 1} of ${product.images.length}`}
                  aria-pressed={index === galleryIndex}
                  onClick={() => setGalleryIndex(index)}
                >
                  <img src={image} alt="" loading="lazy" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="detail-info">
          <p className="detail-brand">
            {product.brand}
            {category ? ` · ${category.name}` : ''}
          </p>
          <h1 className="detail-title">{product.name}</h1>
          <RatingStars rating={product.rating} reviewCount={product.reviewCount} size="md" />

          <div className="detail-price-row">
            <span className="detail-price">{formatPrice(product.price)}</span>
            {product.compareAtPrice && (
              <s className="detail-price-old">{formatPrice(product.compareAtPrice)}</s>
            )}
            {discount !== null && <span className="detail-discount">Save {discount}%</span>}
          </div>

          <p className="detail-stock">
            {product.inStock
              ? 'In stock — dispatched within one working day'
              : 'Currently out of stock'}
          </p>

          <p className="detail-desc">{product.shortDescription}</p>

          <ul className="detail-highlights">
            {product.highlights.map((highlight) => (
              <li key={highlight}>{highlight}</li>
            ))}
          </ul>

          <div className="detail-actions">
            <div className="qty-selector" aria-label="Quantity">
              <button
                type="button"
                className="qty-btn"
                onClick={() => setQuantity((current) => Math.max(1, current - 1))}
                disabled={quantity <= 1}
                aria-label="Decrease quantity"
              >
                &minus;
              </button>
              <input
                className="qty-input"
                type="number"
                min={1}
                max={99}
                value={quantity}
                aria-label="Quantity"
                onChange={(event) => {
                  const next = Number(event.target.value);
                  if (Number.isInteger(next) && next >= 1 && next <= 99) {
                    setQuantity(next);
                  }
                }}
              />
              <button
                type="button"
                className="qty-btn"
                onClick={() => setQuantity((current) => Math.min(99, current + 1))}
                disabled={quantity >= 99}
                aria-label="Increase quantity"
              >
                +
              </button>
            </div>

            {!product.inStock ? (
              <button type="button" className="btn btn-primary btn-lg" disabled>
                Out of stock
              </button>
            ) : !isAuthenticated ? (
              <Link to="/login" className="btn btn-primary btn-lg">
                Sign in to add to cart
              </Link>
            ) : (
              <button
                type="button"
                className="btn btn-primary btn-lg"
                disabled={adding}
                onClick={handleAddToCart}
              >
                {adding ? 'Adding…' : added ? 'Added to cart' : 'Add to cart'}
              </button>
            )}

            {isAuthenticated ? (
              <button
                type="button"
                className={saved ? 'btn btn-outline btn-lg btn-saved' : 'btn btn-outline btn-lg'}
                aria-pressed={saved}
                disabled={wishlistBusy}
                onClick={handleToggleWishlist}
              >
                {wishlistBusy
                  ? 'Saving…'
                  : saved
                    ? 'Saved to wishlist'
                    : 'Add to wishlist'}
              </button>
            ) : (
              <Link to="/login" className="btn btn-outline btn-lg">
                Sign in to save
              </Link>
            )}
          </div>

          {actionError && (
            <p className="form-alert" role="alert">
              {actionError}
            </p>
          )}

          <ul className="detail-panels">
            {DELIVERY_POINTS.map((point) => (
              <li key={point.title} className="detail-panel">
                <span className="detail-panel-icon" aria-hidden="true">
                  <DetailIcon kind={point.icon} />
                </span>
                <div>
                  <h2 className="detail-panel-title">{point.title}</h2>
                  <p className="detail-panel-copy">{point.copy}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <section className="section detail-about" aria-labelledby="about-title">
        <h2 id="about-title" className="section-title">
          About this product
        </h2>
        <p className="detail-about-copy">{product.description}</p>
      </section>

      {related.length > 0 && (
        <section className="section" aria-labelledby="related-title">
          <header className="section-head">
            <div>
              <p className="eyebrow">Pairs well with</p>
              <h2 id="related-title" className="section-title">
                You may also like
              </h2>
            </div>
            <Link to="/products" className="link-arrow">
              All products
            </Link>
          </header>
          <ul className="product-grid product-grid--four">
            {related.map((candidate) => (
              <li key={candidate.id}>
                <ProductCard product={candidate} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
}

export default ProductDetailPage;

