import { Link } from 'react-router-dom';

/** Styled 404 page for unknown routes. */
function NotFoundPage() {
  return (
    <section className="page-section">
      <div className="empty-state-card empty-state-card--centered">
        <p className="eyebrow">Error 404</p>
        <h1 className="empty-state-title">We could not find that page</h1>
        <p className="empty-state-text">
          The link may be outdated or the page may have moved. The full catalogue is one click
          away.
        </p>
        <div className="empty-state-actions">
          <Link to="/" className="btn btn-primary">
            Back to home
          </Link>
          <Link to="/products" className="btn btn-outline">
            Shop all products
          </Link>
        </div>
      </div>
    </section>
  );
}

export default NotFoundPage;
