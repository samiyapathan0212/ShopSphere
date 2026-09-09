package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.domain.Review;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.CreateReviewRequest;
import com.shopsphere.backend.dto.request.UpdateReviewRequest;
import com.shopsphere.backend.dto.response.ReviewPageResponse;
import com.shopsphere.backend.dto.response.ReviewResponse;
import com.shopsphere.backend.dto.response.ReviewSummary;
import com.shopsphere.backend.exception.NotReviewOwnerException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.exception.ReviewAlreadyExistsException;
import com.shopsphere.backend.exception.ReviewNotFoundException;
import com.shopsphere.backend.repository.ProductRepository;
import com.shopsphere.backend.repository.ReviewRepository;
import com.shopsphere.backend.repository.UserRepository;

/**
 * Review operations (Phase 4C). Review ownership is enforced server-side: a user can
 * only create/update/delete their own review. Product listing is paginated and includes
 * average rating and review count.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ReviewPageResponse listReviews(Long productivityId, int page, int size) {
        productRepository.findById(productivityId)
                .orElseThrow(() -> new ProductNotFoundException(productivityId));

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), SortUtils.createdAtDesc());
        Page<Review> pageResult = reviewRepository.findByProductIdOrderByCreatedAtDesc(productivityId, pageable);

        List<ReviewResponse> content = pageResult.getContent().stream()
                .map(this::toReviewResponse)
                .toList();

        return new ReviewPageResponse(content, pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.hasNext(), pageResult.hasPrevious());
    }

    @Transactional(readOnly = true)
    public ReviewSummary reviewSummary(Long productivityId) {
        productRepository.findById(productivityId)
                .orElseThrow(() -> new ProductNotFoundException(productivityId));

        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productivityId, Pageable.unpaged())
                .getContent();
                int count = reviews.size();
        BigDecimal avg = count == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(
                reviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0));
        return new ReviewSummary(productivityId, count, avg);
    }

    @Transactional
    public ReviewResponse createReview(Long userId, Long productivityId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ReviewNotFoundException(userId));
        Product product = productRepository.findById(productivityId)
                .orElseThrow(() -> new ProductNotFoundException(productivityId));

        reviewRepository.findByUserIdAndProductId(userId, productivityId)
                .ifPresent(review -> {
                    throw new ReviewAlreadyExistsException(userId, productivityId);
                });

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.rating())
                .title(request.title())
                .text(request.text())
                .build();
        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getUser().getId().equals(userId)) {
            throw new NotReviewOwnerException(reviewId);
        }

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setText(request.text());
        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getUser().getId().equals(userId)) {
            throw new NotReviewOwnerException(reviewId);
        }
        reviewRepository.delete(review);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getTitle(),
                review.getText(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
