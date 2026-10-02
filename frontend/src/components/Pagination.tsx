import { pageNumbers } from '../mocks/catalogFilters';

interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * Numbered pagination control with prev/next buttons and ellipsis gaps.
 * Purely presentational: the active page and change callback come from the
 * listing page, which will later be driven by the paged catalogue API.
 */
function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  const entries = pageNumbers(page, totalPages);

  return (
    <nav className="pagination" aria-label="Product list pages">
      <button
        type="button"
        className="pagination-btn pagination-btn--step"
        disabled={page === 1}
        onClick={() => onPageChange(page - 1)}
      >
        Previous
      </button>

      <ul className="pagination-list">
        {entries.map((entry, index) =>
          entry === null ? (
            <li key={`gap-${index}`} className="pagination-gap" aria-hidden="true">
              &#8230;
            </li>
          ) : (
            <li key={entry}>
              <button
                type="button"
                className={
                  entry === page ? 'pagination-btn pagination-btn--active' : 'pagination-btn'
                }
                aria-current={entry === page ? 'page' : undefined}
                onClick={() => onPageChange(entry)}
              >
                {entry}
              </button>
            </li>
          ),
        )}
      </ul>

      <button
        type="button"
        className="pagination-btn pagination-btn--step"
        disabled={page === totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}

export default Pagination;
