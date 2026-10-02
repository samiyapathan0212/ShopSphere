import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Pagination from '../components/Pagination';
import ProductCard from '../components/ProductCard';
import { MOCK_CATEGORIES, MOCK_PRODUCTS } from '../mocks/catalog';
import {
  DEFAULT_PAGE_SIZE,
  PRICE_RANGES,
  SORT_OPTIONS,
  queryMockCatalog,
  type PriceRangeId,
  type SortKey,
} from '../mocks/catalogFilters';

function readSort(value: string | null): SortKey {
  const match = SORT_OPTIONS.find((option) => option.id === value);
  return match ? match.id : 'featured';
}

function readPriceRange(value: string | null): PriceRangeId {
  const match = PRICE_RANGES.find((range) => range.id === value);
  return match ? match.id : 'all';
}

/**
 * A category slug is honoured only when it exists in the mock catalog. Unknown
 * slugs fall back to the full catalogue so the URL, heading, active filter and
 * product grid always agree with each other.
 */
function readCategory(value: string | null): string {
  return value && MOCK_CATEGORIES.some((category) => category.id === value) ? value : 'all';
}

/**
 * Product listing (Phase 8). The toolbar is real UI backed by the isolated mock
 * catalog: search, category, price and sort selections are applied locally so the
 * layout behaves like the finished storefront. The URL is the single source of
 * truth for the active filters, so links like /products?category=home always
 * render Home & Kitchen even when the page is already mounted. When the
 * catalogue API lands these controls become RTK Query arguments and the query
 * helpers are removed.
 */
function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const activeQuery = searchParams.get('q') ?? '';
  const categoryId = readCategory(searchParams.get('category'));
  const priceRange = readPriceRange(searchParams.get('price'));
  const sort = readSort(searchParams.get('sort'));

  const [searchInput, setSearchInput] = useState(activeQuery);
  const [page, setPage] = useState(1);

  // Changing any filter produces a fresh result set: jump back to the first page.
  useEffect(() => {
    setPage(1);
  }, [searchParams]);

  // Keep the visible search field in step with the URL (e.g. navbar searches).
  useEffect(() => {
    setSearchInput(activeQuery);
  }, [activeQuery]);

  const result = useMemo(
    () =>
      queryMockCatalog({
        search: activeQuery,
        categoryId,
        priceRange,
        sort,
        page,
        pageSize: DEFAULT_PAGE_SIZE,
      }),
    [activeQuery, categoryId, priceRange, sort, page],
  );

  // Keep the URL in step with the toolbar so category links stay shareable.
  const syncUrl = (next: { q?: string; category?: string; price?: string; sort?: string }) => {
    const params = new URLSearchParams();
    const nextSearch = (next.q ?? activeQuery).trim();
    const nextCategory = next.category ?? categoryId;
    const nextPrice = next.price ?? priceRange;
    const nextSort = next.sort ?? sort;

    if (nextSearch) {
      params.set('q', nextSearch);
    }
    if (nextCategory !== 'all') {
      params.set('category', nextCategory);
    }
    if (nextPrice !== 'all') {
      params.set('price', nextPrice);
    }
    if (nextSort !== 'featured') {
      params.set('sort', nextSort);
    }
    setSearchParams(params, { replace: true });
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(1);
    syncUrl({ q: searchInput });
  };

  const applyCategory = (nextCategory: string) => {
    syncUrl({ category: nextCategory });
  };

  const clearFilters = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams(), { replace: true });
  };

  const hasFilters =
    Boolean(activeQuery.trim()) || categoryId !== 'all' || priceRange !== 'all' || sort !== 'featured';
  const activeCategory = MOCK_CATEGORIES.find((category) => category.id === categoryId);

  return (
    <>
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        {activeCategory ? (
          <>
            <Link to="/products">All products</Link>
            <span aria-hidden="true">/</span>
            <span>{activeCategory.name}</span>
          </>
        ) : (
          <span>All products</span>
        )}
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">{activeCategory ? activeCategory.name : 'All products'}</h1>
          <p className="page-lead">
            {result.totalItems} of {MOCK_PRODUCTS.length} products
            {activeCategory ? ` in ${activeCategory.name}` : ''}
            {activeQuery.trim() ? ` matching "${activeQuery.trim()}"` : ''}.
          </p>
          <p className="demo-note">
            Demo catalogue — sample products for interface preview only, not real inventory.
          </p>
        </div>
      </header>

      <div className="catalog-layout">
        <aside className="filters" aria-label="Product filters">
          <div className="filter-panel-head">
            <h2 className="filter-panel-title">Filters</h2>
            <span className="filter-panel-count">
              {result.totalItems} {result.totalItems === 1 ? 'product' : 'products'}
            </span>
          </div>

          <section className="filter-block">
            <h2 className="filter-title">Search</h2>
            <form className="filter-search" role="search" onSubmit={submitSearch}>
              <label className="visually-hidden" htmlFor="catalog-search">
                Search products
              </label>
              <input
                id="catalog-search"
                className="form-input"
                type="search"
                placeholder="Search products"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
              <button type="submit" className="btn btn-primary btn-sm">
                Search
              </button>
            </form>
          </section>

          <section className="filter-block">
            <h2 className="filter-title">Category</h2>
            <ul className="filter-list filter-list--chips">
              <li>
                <button
                  type="button"
                  className={
                    categoryId === 'all' ? 'filter-chip filter-chip--active' : 'filter-chip'
                  }
                  onClick={() => applyCategory('all')}
                >
                  All categories
                  <span className="filter-chip-count">{MOCK_PRODUCTS.length}</span>
                </button>
              </li>
              {MOCK_CATEGORIES.map((category) => (
                <li key={category.id}>
                  <button
                    type="button"
                    className={
                      categoryId === category.id
                        ? 'filter-chip filter-chip--active'
                        : 'filter-chip'
                    }
                    onClick={() => applyCategory(category.id)}
                  >
                    {category.name}
                    <span className="filter-chip-count">
                      {MOCK_PRODUCTS.filter((product) => product.categoryId === category.id).length}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </section>

          <section className="filter-block">
            <h2 className="filter-title">Price</h2>
            <ul className="filter-list">
              {PRICE_RANGES.map((range) => (
                <li key={range.id}>
                  <label className="filter-radio">
                    <input
                      type="radio"
                      name="price-range"
                      value={range.id}
                      checked={priceRange === range.id}
                      onChange={() => syncUrl({ price: range.id })}
                    />
                    <span>{range.label}</span>
                  </label>
                </li>
              ))}
            </ul>
          </section>

          {hasFilters && (
            <button type="button" className="btn btn-ghost btn-sm btn-block" onClick={clearFilters}>
              Clear all filters
            </button>
          )}
        </aside>

        <section className="catalog-results" aria-label="Product results">
          <div className="results-chips" role="group" aria-label="Quick category filters">
            <button
              type="button"
              className={categoryId === 'all' ? 'filter-chip filter-chip--active' : 'filter-chip'}
              onClick={() => applyCategory('all')}
            >
              All
            </button>
            {MOCK_CATEGORIES.map((category) => (
              <button
                key={category.id}
                type="button"
                className={
                  categoryId === category.id ? 'filter-chip filter-chip--active' : 'filter-chip'
                }
                onClick={() => applyCategory(category.id)}
              >
                {category.name}
              </button>
            ))}
          </div>

          <div className="results-toolbar">
            <p className="results-count">
              <strong>{result.totalItems}</strong>{' '}
              {result.totalItems === 1 ? 'product' : 'products'} · page{' '}
              <strong>{result.page}</strong> of {result.totalPages}
            </p>

            <div className="results-sort">
              <label className="form-label" htmlFor="catalog-sort">
                Sort by
              </label>
              <select
                id="catalog-sort"
                className="form-input form-select"
                value={sort}
                onChange={(event) => {
                  syncUrl({ sort: readSort(event.target.value) });
                }}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {result.items.length === 0 ? (
            <div className="empty-state">
              <h2 className="empty-state-title">No products match these filters</h2>
              <p className="empty-state-text">
                Try a broader search term, or clear the filters to see the full catalogue again.
              </p>
              <button type="button" className="btn btn-primary" onClick={clearFilters}>
                Clear filters
              </button>
            </div>
          ) : (
            <>
              <ul className="product-grid product-grid--three">
                {result.items.map((product) => (
                  <li key={product.id}>
                    <ProductCard product={product} />
                  </li>
                ))}
              </ul>

              <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
            </>
          )}
        </section>
      </div>
    </>
  );
}

export default ProductsPage;