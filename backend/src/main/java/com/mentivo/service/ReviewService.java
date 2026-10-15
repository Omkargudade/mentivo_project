package com.mentivo.service;

import com.mentivo.dto.review.CreateReviewRequest;
import com.mentivo.dto.review.ReviewDto;
import com.mentivo.entity.Booking;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.Review;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.ReviewRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         MentorProfileRepository mentorProfileRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewDto createReview(Long learnerId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (!booking.getLearner().getId().equals(learnerId)) {
            throw new UnauthorizedException("Only the booking learner can submit a review");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Reviews can only be submitted for COMPLETED sessions");
        }

        Optional<Review> existingReview = reviewRepository.findByBookingId(booking.getId());
        if (existingReview.isPresent()) {
            throw new ConflictException("You have already reviewed this session");
        }

        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner not found"));
        MentorProfile mentor = booking.getMentor();

        Review review = new Review(booking, mentor, learner, request.getRating(), request.getReviewText());
        Review saved = reviewRepository.save(review);

        // Update mentor's average rating and review count
        Double avgRating = reviewRepository.calculateAverageRatingForMentor(mentor.getId());
        long totalCount = reviewRepository.countByMentorId(mentor.getId());

        if (avgRating != null) {
            BigDecimal rounded = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP);
            mentor.setAverageRating(rounded.doubleValue());
        }
        mentor.setTotalReviews((int) totalCount);
        mentorProfileRepository.save(mentor);

        notificationService.createNotification(
                mentor.getUser(),
                "New " + request.getRating() + "-Star Review!",
                learner.getFullName() + " left you a review: \"" +
                        (request.getReviewText() != null && request.getReviewText().length() > 60
                                ? request.getReviewText().substring(0, 57) + "..."
                                : request.getReviewText()) + "\"",
                NotificationType.NEW_REVIEW,
                saved.getId()
        );

        return DtoMapper.toReviewDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getMentorReviews(Long mentorId, Pageable pageable) {
        return reviewRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, pageable)
                .map(DtoMapper::toReviewDto);
    }
}
