import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import RatingStars from './RatingStars';
import { useAddItemMutation } from '../app/api/cartApi';
import {
  useAddWishlistItemMutation,
  useGetWishlistQuery,
  useRemoveWishlistItemMutation,
} from '../app/api/wishlistApi';
import { describeApiError } from '../app/api/authApi';
import type { RootState } from '../app/store';
import { discountPercent, findCategoryById, formatPrice, primaryImage, type MockProduct } from '../mocks/catalog';

interface ProductCardProps {
  product: MockProduct;
}

function badgeClass(badge: NonNullable<MockProduct['badge']>): string {
  return `product-badge product-badge--${badge.toLowerCase().replace(/\s+/g, '-')}`;
}

/**
 * Storefront product card. Presentational: renders a mock product with imagery,
 * badges, rating, pricing and the add-to-cart / wishlist actions. Add to cart and
 * the heart toggle both go through the real cart and wishlist APIs (a customer
 * account is required, so signed-out visitors get a sign-in call to action); the
 * "Added" acknowledgement is local state that just confirms the API round trip.
 */
function ProductCard({ product }: ProductCardProps) {
  const [added, setAdded] = useState(false);
  // If the demo image fails to load, a neutral tile keeps the card dimensions.
  const [imageFailed, setImageFailed] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));
  const [addItem, { isLoading: adding }] = useAddItemMutation();
  // The saved state comes from the server wishlist, not local memory, so the
  // heart stays filled on every card after a reload and matches the badge.
  const wishlistQuery = useGetWishlistQuery(undefined, { skip: !isAuthenticated });
  const [addWishlistItem, { isLoading: wishlistAdding }] = useAddWishlistItemMutation();
  const [removeWishlistItem, { isLoading: wishlistRemoving }] = useRemoveWishlistItemMutation();
  const saved =
    wishlistQuery.data?.items.some((item) => item.productId === product.id) ?? false;
  const wishlistBusy = wishlistAdding || wishlistRemoving;
  const discount = discountPercent(product);
  const categoryName = findCategoryById(product.categoryId)?.name;

  useEffect(() => {
    if (!added) {
      return undefined;
    }
    const timer = window.setTimeout(() => setAdded(false), 1500);
    return () => window.clearTimeout(timer);
  }, [added]);

  // Guarded against double clicks: the button is disabled while the mutation
  // is in flight, so the in-flight check is a second line of defence.
  const handleAdd = async () => {
    if (adding || !product.inStock) {
      return;
    }
    setActionError(null);
    try {
      await addItem({ productId: product.id, quantity: 1 }).unwrap();
      setAdded(true);
    } catch (error) {
      setActionError(describeApiError(error, 'Could not add this product to the cart.'));
    }
  };

  // The heart toggles against the server wishlist. The displayed state is
  // derived from the query result, so the button only changes once the API call
  // has actually succeeded.
  const handleToggleWishlist = async () => {
    if (wishlistBusy) {
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
      setActionError(
        describeApiError(error, 'Could not update your wishlist. Please try again.'),
      );
    }
  };

  return (
    <article className="product-card">
      <div className="product-card-media">
        <Link to={`/products/${product.id}`} className="product-card-media-link" tabIndex={-1}>
          {imageFailed ? (
            <span className="product-card-img-fallback">Image unavailable</span>
          ) : (
            <img
              className="product-card-img"
              src={primaryImage(product)}
              alt={product.name}
              loading="lazy"
              onError={() => setImageFailed(true)}
            />
          )}
        </Link>

        {(product.badge || discount !== null) && (
          <div className="product-card-flags">
            {product.badge && <span className={badgeClass(product.badge)}>{product.badge}</span>}
            {discount !== null && <span className="product-discount">-{discount}%</span>}
          </div>
        )}

        {!product.inStock && (
          <span className="product-stock product-stock--out">Out of stock</span>
        )}

        {isAuthenticated ? (
          <button
            type="button"
            className={saved ? 'product-wishlist product-wishlist--active' : 'product-wishlist'}
            aria-pressed={saved}
            disabled={wishlistBusy}
            title={actionError ?? undefined}
            aria-label={
              saved ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`
            }
            onClick={handleToggleWishlist}
          >
            <svg viewBox="0 0 24 24" width="17" height="17" aria-hidden="true" focusable="false">
              <path
                d="M12 20s-7-4.3-7-9.3A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19 10.7c0 5-7 9.3-7 9.3Z"
                fill={saved ? 'currentColor' : 'none'}
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        ) : (
          <Link
            to="/login"
            className="product-wishlist"
            aria-label={`Sign in to save ${product.name} to your wishlist`}
          >
            <svg viewBox="0 0 24 24" width="17" height="17" aria-hidden="true" focusable="false">
              <path
                d="M12 20s-7-4.3-7-9.3A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19 10.7c0 5-7 9.3-7 9.3Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinejoin="round"
              />
            </svg>
          </Link>
        )}
      </div>

      <div className="product-card-body">
        <p className="product-meta">
          <span className="product-brand">{product.brand}</span>
          {categoryName && (
            <>
              <span className="product-meta-dot" aria-hidden="true">
                ·
              </span>
              <span className="product-category">{categoryName}</span>
            </>
          )}
        </p>
        <h3 className="product-name">
          <Link to={`/products/${product.id}`}>{product.name}</Link>
        </h3>
        <RatingStars rating={product.rating} reviewCount={product.reviewCount} />

        <p className="product-price-row">
          <span className="product-price">{formatPrice(product.price)}</span>
          {product.compareAtPrice && (
            <s className="product-price-old">{formatPrice(product.compareAtPrice)}</s>
          )}
        </p>

        {!product.inStock ? (
          <button
            type="button"
            className="btn btn-primary btn-sm btn-block product-card-action"
            disabled
          >
            Out of stock
          </button>
        ) : !isAuthenticated ? (
          <Link to="/login" className="btn btn-outline btn-sm btn-block product-card-action">
            Sign in to add
          </Link>
        ) : (
          <button
            type="button"
            className="btn btn-primary btn-sm btn-block product-card-action"
            disabled={adding}
            title={actionError ?? undefined}
            onClick={handleAdd}
          >
            {adding ? 'Adding…' : added ? 'Added to cart' : actionError ? 'Try again' : 'Add to cart'}
          </button>
        )}
      </div>
    </article>
  );
}

export default ProductCard;
