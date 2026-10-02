import { MOCK_PRODUCTS, type MockProduct } from './catalog';

/**
 * UI-only query helpers for the storefront grid (Phase 8).
 *
 * These functions exist purely so the Products page can demonstrate search,
 * filtering, sorting and pagination against local mock data. When the catalog
 * API is available these parameters are sent to the server and this module is
 * removed — pages will then read paged results from RTK Query instead.
 */

export type SortKey = 'featured' | 'price-asc' | 'price-desc' | 'rating' | 'name-asc';

export interface MockCatalogQuery {
  search: string;
  categoryId: string | 'all';
  priceRange: PriceRangeId;
  sort: SortKey;
  page: number;
  pageSize: number;
}

export interface MockCatalogResult {
  items: MockProduct[];
  totalItems: number;
  totalPages: number;
  page: number;
  pageSize: number;
}

export type PriceRangeId = 'all' | 'under-100' | '100-250' | '250-500' | 'over-500';

export const PRICE_RANGES: { id: PriceRangeId; label: string; min?: number; max?: number }[] = [
  { id: 'all', label: 'Any price' },
  { id: 'under-100', label: 'Under $100', max: 100 },
  { id: '100-250', label: '$100 to $250', min: 100, max: 250 },
  { id: '250-500', label: '$250 to $500', min: 250, max: 500 },
  { id: 'over-500', label: 'Over $500', min: 500 },
];

export const SORT_OPTIONS: { id: SortKey; label: string }[] = [
  { id: 'featured', label: 'Featured' },
  { id: 'price-asc', label: 'Price: low to high' },
  { id: 'price-desc', label: 'Price: high to low' },
  { id: 'rating', label: 'Customer rating' },
  { id: 'name-asc', label: 'Name: A to Z' },
];

export const DEFAULT_PAGE_SIZE = 8;

export const DEFAULT_QUERY: MockCatalogQuery = {
  search: '',
  categoryId: 'all',
  priceRange: 'all',
  sort: 'featured',
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
};

function matchesPrice(product: MockProduct, range: PriceRangeId): boolean {
  const rangeDefinition = PRICE_RANGES.find((candidate) => candidate.id === range);
  if (!rangeDefinition) {
    return true;
  }
  const { min, max } = rangeDefinition;
  if (typeof min === 'number' && product.price < min) {
    return false;
  }
  if (typeof max === 'number' && product.price > max) {
    return false;
  }
  return true;
}

function matchesSearch(product: MockProduct, search: string): boolean {
  const term = search.trim().toLowerCase();
  if (!term) {
    return true;
  }
  return (
    product.name.toLowerCase().includes(term) ||
    product.brand.toLowerCase().includes(term) ||
    product.shortDescription.toLowerCase().includes(term)
  );
}

function sortProducts(products: MockProduct[], sort: SortKey): MockProduct[] {
  const sorted = [...products];
  switch (sort) {
    case 'price-asc':
      return sorted.sort((a, b) => a.price - b.price);
    case 'price-desc':
      return sorted.sort((a, b) => b.price - a.price);
    case 'rating':
      return sorted.sort((a, b) => b.rating - a.rating);
    case 'name-asc':
      return sorted.sort((a, b) => a.name.localeCompare(b.name));
    case 'featured':
    default:
      // Featured keeps the curated mock order, so trending items surface first.
      return sorted;
  }
}

/** Applies the mock query and returns one page of display-ready products. */
export function queryMockCatalog(query: MockCatalogQuery): MockCatalogResult {
  const filtered = MOCK_PRODUCTS.filter(
    (product) =>
      matchesSearch(product, query.search) &&
      (query.categoryId === 'all' || product.categoryId === query.categoryId) &&
      matchesPrice(product, query.priceRange),
  );

  const sorted = sortProducts(filtered, query.sort);
  const pageSize = Math.max(1, query.pageSize);
  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const page = Math.min(Math.max(1, query.page), totalPages);
  const start = (page - 1) * pageSize;

  return {
    items: sorted.slice(start, start + pageSize),
    totalItems: sorted.length,
    totalPages,
    page,
    pageSize,
  };
}

/** Compact page numbers for the pagination control (with `null` gaps). */
export function pageNumbers(current: number, total: number): (number | null)[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }
  const pages = new Set<number>([1, total, current, current - 1, current + 1]);
  const ordered = [...pages].filter((page) => page >= 1 && page <= total).sort((a, b) => a - b);

  const withGaps: (number | null)[] = [];
  ordered.forEach((page, index) => {
    if (index > 0 && page - ordered[index - 1] > 1) {
      withGaps.push(null);
    }
    withGaps.push(page);
  });
  return withGaps;
}