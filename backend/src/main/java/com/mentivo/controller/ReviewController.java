package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.review.CreateReviewRequest;
import com.mentivo.dto.review.ReviewDto;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews & Ratings", description = "Post-session reviews, ratings, and feedback")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Submit a 1-5 star review for a completed session")
    public ResponseEntity<ApiResponse<ReviewDto>> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReviewDto review = reviewService.createReview(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thank you for your review!", review));
    }

    @GetMapping("/mentor/{mentorId}")
    @Operation(summary = "Get reviews for a mentor")
    public ResponseEntity<ApiResponse<Page<ReviewDto>>> getMentorReviews(
            @PathVariable Long mentorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewDto> reviews = reviewService.getMentorReviews(mentorId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}
