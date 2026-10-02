interface RatingStarsProps {
  rating: number;
  reviewCount?: number;
  /** Compact variant is used inside product cards. */
  size?: 'sm' | 'md';
}

/**
 * Star rating display. Uses the star glyph rather than an image so it scales with
 * the font, and exposes the numeric rating to assistive technology.
 */
function RatingStars({ rating, reviewCount, size = 'sm' }: RatingStarsProps) {
  const rounded = Math.round(rating * 2) / 2;
  const stars = [1, 2, 3, 4, 5];

  return (
    <span className={`rating rating--${size}`}>
      <span className="rating-stars" role="img" aria-label={`Rated ${rating} out of 5`}>
        {stars.map((star) => (
          <span
            key={star}
            aria-hidden="true"
            className={
              rounded >= star
                ? 'rating-star rating-star--full'
                : rounded >= star - 0.5
                  ? 'rating-star rating-star--half'
                  : 'rating-star'
            }
          >
            &#9733;
          </span>
        ))}
      </span>
      <span className="rating-value">{rating.toFixed(1)}</span>
      {typeof reviewCount === 'number' && (
        <span className="rating-count">({reviewCount.toLocaleString('en-US')})</span>
      )}
    </span>
  );
}

export default RatingStars;