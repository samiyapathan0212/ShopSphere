import { useState } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import {
  IMG,
  MOCK_CATEGORIES,
  findCategoryById,
  getBestSellerProducts,
  getTrendingProducts,
  unsplash,
} from '../mocks/catalog';

type ServiceIconKind = 'lock' | 'return' | 'box' | 'truck' | 'check';

function ServiceIcon({ kind }: { kind: ServiceIconKind }) {
  return (
    <svg viewBox="0 0 24 24" width="19" height="19" aria-hidden="true" focusable="false">
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
      {kind === 'box' && (
        <>
          <path
            d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
          <path
            d="M4 7.5l8 4.5 8-4.5M12 12v9"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
        </>
      )}
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
      {kind === 'check' && (
        <>
          <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
          <path
            d="m8.5 12.2 2.3 2.3 4.7-4.7"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
    </svg>
  );
}

/** Compact trust strip under the hero — factual service points only. */
const TRUST_ITEMS: { icon: ServiceIconKind; label: string }[] = [
  { icon: 'truck', label: 'Free shipping over $50' },
  { icon: 'return', label: 'Easy 30-day returns' },
  { icon: 'lock', label: 'Secure payments' },
  { icon: 'box', label: 'Stock-aware catalogue' },
];

/** Editorial lifestyle trio — existing categories with a short phrase. */
const LIFESTYLE_IDS = ['audio', 'workspace', 'home'] as const;

const LIFESTYLE_PHRASES: Record<string, string> = {
  audio: 'Sound for every room',
  workspace: 'A better desk, every day',
  home: 'Everyday kitchen classics',
};

/** Why ShopSphere — platform qualities, no invented statistics. */
const BENEFITS: { icon: ServiceIconKind; title: string; copy: string }[] = [
  {
    icon: 'box',
    title: 'Curated everyday products',
    copy: 'A focused range picked for daily use — not an endless warehouse of near-identical listings.',
  },
  {
    icon: 'check',
    title: 'Transparent pricing',
    copy: 'The price you see is the price you pay, and discounts appear only where the catalogue marks them down.',
  },
  {
    icon: 'lock',
    title: 'Secure checkout',
    copy: 'Card details are handled over an encrypted connection by a tokenised payment provider.',
  },
  {
    icon: 'return',
    title: 'Easy returns',
    copy: 'Changed your mind? Start a 30-day return from your account, free of charge.',
  },
];

/**
 * Storefront homepage. The product and category data comes from the isolated
 * mock catalog so it can be swapped for the catalogue API later without
 * touching this layout. The newsletter form is visual only: it never claims a
 * subscription succeeded.
 */
function HomePage() {
  const trending = getTrendingProducts(6);
  const bestSellers = getBestSellerProducts(4);
  const promoImage = findCategoryById('outdoors')?.image;
  const lifestyleTiles = LIFESTYLE_IDS.flatMap((id) => {
    const category = findCategoryById(id);
    return category ? [category] : [];
  });
  const [newsletterEmail, setNewsletterEmail] = useState('');
  const [newsletterDone, setNewsletterDone] = useState(false);

  return (
    <>
      <section className="hero">
        <img
          className="hero-media"
          src={unsplash(IMG.heroWorkspace, 1800)}
          alt="Curated workspace, audio and home products from the ShopSphere demo catalogue"
        />
        <div className="hero-panel">
          <p className="eyebrow eyebrow--hero">New season</p>
          <h1 className="hero-title">
            Everyday essentials.
            <br />
            Made for the way you live.
          </h1>
          <p className="hero-lead">
            Audio, workspace, home and fitness pieces from one curated demo catalogue — clear
            pricing and honest photography.
          </p>
          <div className="hero-actions">
            <Link to="/products" className="btn btn-primary btn-lg">
              Shop collection
            </Link>
            <a href="#categories-title" className="btn btn-light btn-lg">
              Explore categories
            </a>
          </div>
        </div>
      </section>

      <section className="trust-strip" aria-label="Shopping with ShopSphere">
        <ul className="trust-strip-list">
          {TRUST_ITEMS.map((item) => (
            <li key={item.label}>
              <span className="trust-icon" aria-hidden="true">
                <ServiceIcon kind={item.icon} />
              </span>
              {item.label}
            </li>
          ))}
        </ul>
      </section>

      <section className="section" aria-labelledby="categories-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">The collection</p>
            <h2 id="categories-title" className="section-title">
              Shop by category
            </h2>
            <p className="section-sub">
              Thoughtfully selected essentials for every part of your day.
            </p>
          </div>
          <Link to="/products" className="link-arrow">
            All products
          </Link>
        </header>

        <ul className="category-circles">
          {MOCK_CATEGORIES.map((category) => (
            <li key={category.id}>
              <Link to={`/products?category=${category.id}`} className="category-circle">
                <span className="category-circle-media">
                  <img src={category.image} alt={category.name} loading="lazy" />
                </span>
                <span className="category-circle-name">{category.name}</span>
                <span className="category-circle-count">{category.itemCount} products</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="section" aria-labelledby="featured-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">Customer favourites</p>
            <h2 id="featured-title" className="section-title">
              Best sellers
            </h2>
            <p className="section-sub">
              The four products the demo catalogue flags as best sellers, at their listed prices.
            </p>
          </div>
          <Link to="/products" className="link-arrow">
            View all
          </Link>
        </header>

        <ul className="product-grid product-grid--four">
          {bestSellers.map((product) => (
            <li key={product.id}>
              <ProductCard product={product} />
            </li>
          ))}
        </ul>
      </section>

      <section className="section" aria-labelledby="editorial-title">
        <div className="editorial-banner">
          <div className="editorial-banner-media">
            {promoImage && (
              <img
                src={promoImage}
                alt="Outdoor gear from the demo catalogue photographed on location"
                loading="lazy"
              />
            )}
          </div>
          <div className="editorial-banner-content">
            <p className="eyebrow">ShopSphere edit</p>
            <h2 id="editorial-title">Made for the way you live.</h2>
            <p>
              Smart essentials for work, home, movement and everyday moments — every piece from the
              same curated demo catalogue.
            </p>
            <Link to="/products" className="link-arrow link-arrow--light">
              Shop the collection
            </Link>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="trending-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">Popular right now</p>
            <h2 id="trending-title" className="section-title">
              Trending now
            </h2>
            <p className="section-sub">Swipe through the demo catalogue's current favourites.</p>
          </div>
          <Link to="/products?sort=rating" className="link-arrow">
            Top rated
          </Link>
        </header>

        <ul className="product-scroller">
          {trending.map((product) => (
            <li key={product.id} className="product-scroller-item">
              <ProductCard product={product} />
            </li>
          ))}
        </ul>
      </section>

      <section className="section" aria-labelledby="shopby-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">In the spotlight</p>
            <h2 id="shopby-title" className="section-title">
              Three corners of the collection
            </h2>
            <p className="section-sub">
              Audio, workspace and home — three ways into the catalogue.
            </p>
          </div>
          <Link to="/products" className="link-arrow">
            See everything
          </Link>
        </header>

        <ul className="lifestyle-grid">
          {lifestyleTiles.map((category) => (
            <li key={category.id}>
              <Link to={`/products?category=${category.id}`} className="cat-tile cat-tile--large">
                <img src={category.image} alt={category.name} loading="lazy" />
                <span className="cat-content">
                  <span className="cat-name">{category.name}</span>
                  <span className="cat-tagline">{LIFESTYLE_PHRASES[category.id]}</span>
                  <span className="cat-count">{category.itemCount} products</span>
                </span>
                <span className="cat-cta">
                  Shop now
                  <span aria-hidden="true">&rarr;</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="section" aria-labelledby="benefits-title">
        <header className="section-head">
          <div>
            <p className="eyebrow">Why ShopSphere</p>
            <h2 id="benefits-title" className="section-title">
              Built for straightforward shopping
            </h2>
          </div>
        </header>

        <ul className="benefit-grid">
          {BENEFITS.map((benefit) => (
            <li key={benefit.title} className="benefit-card">
              <span className="benefit-icon" aria-hidden="true">
                <ServiceIcon kind={benefit.icon} />
              </span>
              <h3 className="benefit-title">{benefit.title}</h3>
              <p className="benefit-copy">{benefit.copy}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="section">
        <div className="newsletter">
          <div className="newsletter-copy">
            <h2 className="newsletter-title">Stay in the loop.</h2>
            <p className="newsletter-text">
              Get new arrivals and curated picks delivered occasionally.
            </p>
          </div>
          <form
            className="newsletter-form"
            onSubmit={(event) => {
              event.preventDefault();
              setNewsletterDone(true);
            }}
          >
            <label className="visually-hidden" htmlFor="newsletter-email">
              Email address
            </label>
            <input
              id="newsletter-email"
              className="newsletter-input"
              type="email"
              required
              placeholder="you@example.com"
              value={newsletterEmail}
              onChange={(event) => setNewsletterEmail(event.target.value)}
            />
            <button type="submit" className="btn btn-primary">
              Subscribe
            </button>
          </form>
          <p className="newsletter-note" role="status">
            {newsletterDone
              ? 'Demo storefront — no emails are sent and nothing was subscribed.'
              : 'Demo storefront · sample catalogue data'}
          </p>
        </div>
      </section>
    </>
  );
}

export default HomePage;