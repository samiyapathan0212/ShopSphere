package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.CreateReviewRequest;
import com.shopsphere.backend.dto.request.UpdateReviewRequest;
import com.shopsphere.backend.dto.response.ReviewPageResponse;
import com.shopsphere.backend.dto.response.ReviewResponse;
import com.shopsphere.backend.dto.response.ReviewSummary;
import com.shopsphere.backend.service.ReviewService;

/**
 * Review endpoints (Phase 4C). Customers can create/update/delete only their own
 * reviews. Review listing is paginated and includes average rating and review count.
 * Mutations require CUSTOMER authentication.
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/products/{productId}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReviewPageResponse> listReviews(
                        @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.listReviews(productId, page, size));
    }

    @GetMapping("/products/{productId}/reviews/summary")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
        public ResponseEntity<ReviewSummary> reviewSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.reviewSummary(productId));
    }

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<ReviewResponse> createReview(Principal principal,
                                                       @PathVariable Long productId,
                                                       @Valid @RequestBody CreateReviewRequest request) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(reviewService.createReview(userId, productId, request));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(Principal principal,
                                                       @PathVariable Long reviewId,
                                                                                                               @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteReview(Principal principal, @PathVariable Long reviewId) {
        Long userId = userIdFrom(principal);
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private Long userIdFrom(Principal principal) {
        return ((com.shopsphere.backend.security.UserPrincipal) principal).user().getId();
    }
}
