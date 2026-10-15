package com.mentivo.service;

import com.mentivo.dto.call.CallSessionCompleteRequest;
import com.mentivo.dto.call.CallSessionCompleteResponse;
import com.mentivo.entity.Booking;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.ReviewRepository;
import com.mentivo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class CallSessionService {

    private static final Logger log = LoggerFactory.getLogger(CallSessionService.class);

    private final BookingRepository bookingRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public CallSessionService(BookingRepository bookingRepository,
                              MentorProfileRepository mentorProfileRepository,
                              UserRepository userRepository,
                              ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public CallSessionCompleteResponse completeCallSession(Long currentUserId, CallSessionCompleteRequest request) {
        if (request.getDurationSeconds() == null || request.getDurationSeconds() < 5) {
            log.info("Call {} completed but duration {}s was under minimum threshold of 5s",
                    request.getCallId(), request.getDurationSeconds());
            return CallSessionCompleteResponse.notEligible("Call duration was under minimum threshold for review");
        }

        if (request.getPeerUserId() == null || request.getPeerUserId().equals(currentUserId)) {
            return CallSessionCompleteResponse.notEligible("Invalid peer participant");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User peerUser = userRepository.findById(request.getPeerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Peer user not found"));

        // Determine who is learner and who is mentor
        MentorProfile mentorProfile = null;
        User learner = null;

        if (peerUser.getMentorProfile() != null && currentUser.getMentorProfile() == null) {
            mentorProfile = peerUser.getMentorProfile();
            learner = currentUser;
        } else if (currentUser.getMentorProfile() != null && peerUser.getMentorProfile() == null) {
            mentorProfile = currentUser.getMentorProfile();
            learner = peerUser;
        } else if (peerUser.getMentorProfile() != null) {
            mentorProfile = peerUser.getMentorProfile();
            learner = currentUser;
        } else if (currentUser.getMentorProfile() != null) {
            mentorProfile = currentUser.getMentorProfile();
            learner = peerUser;
        } else {
            return CallSessionCompleteResponse.notEligible("Neither participant has an active mentor profile");
        }

        Booking targetBooking = null;

        // If specific booking ID was provided
        if (request.getBookingId() != null) {
            Booking b = bookingRepository.findById(request.getBookingId()).orElse(null);
            if (b != null && b.getLearner().getId().equals(learner.getId()) && b.getMentor().getId().equals(mentorProfile.getId())) {
                targetBooking = b;
            }
        }

        // Look for today's active booking between learner and mentor
        if (targetBooking == null) {
            List<Booking> activeBookings = bookingRepository.findByMentorIdAndBookingDate(mentorProfile.getId(), LocalDate.now());
            final Long learnerId = learner.getId();
            targetBooking = activeBookings.stream()
                    .filter(b -> b.getLearner().getId().equals(learnerId))
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);
        }

        // If no booking existed (e.g. instant 1-to-1 WebRTC call from chat), create a completed booking record
        if (targetBooking == null) {
            targetBooking = new Booking();
            targetBooking.setLearner(learner);
            targetBooking.setMentor(mentorProfile);
            targetBooking.setBookingDate(LocalDate.now());
            targetBooking.setStartTime(LocalTime.now().minusSeconds(request.getDurationSeconds()));
            targetBooking.setEndTime(LocalTime.now());
            int mins = Math.max(1, (int) Math.ceil(request.getDurationSeconds() / 60.0));
            targetBooking.setDurationMinutes(mins);
            targetBooking.setHourlyRate(BigDecimal.ZERO);
            targetBooking.setSessionPrice(BigDecimal.ZERO);
            targetBooking.setPlatformFee(BigDecimal.ZERO);
            targetBooking.setTotalAmount(BigDecimal.ZERO);
            targetBooking.setMentorEarnings(BigDecimal.ZERO);
            targetBooking.setStatus(BookingStatus.COMPLETED);
            targetBooking.setMeetingNotes("Direct 1-to-1 WebRTC Mentoring Call (" + request.getDurationSeconds() + "s)");
            targetBooking = bookingRepository.save(targetBooking);

            mentorProfile.setSessionsCompleted(mentorProfile.getSessionsCompleted() + 1);
            mentorProfileRepository.save(mentorProfile);
            log.info("Created and completed instant session booking ID {} for mentor ID {} and learner ID {}",
                    targetBooking.getId(), mentorProfile.getId(), learner.getId());
        } else {
            if (targetBooking.getStatus() != BookingStatus.COMPLETED) {
                targetBooking.setStatus(BookingStatus.COMPLETED);
                targetBooking = bookingRepository.save(targetBooking);

                mentorProfile.setSessionsCompleted(mentorProfile.getSessionsCompleted() + 1);
                mentorProfileRepository.save(mentorProfile);
                log.info("Marked existing booking ID {} as COMPLETED", targetBooking.getId());
            }
        }

        String mentorName = mentorProfile.getUser() != null ? mentorProfile.getUser().getFullName() : "Mentor";

        // Only the learner participant is eligible to submit a review
        boolean isLearner = currentUser.getId().equals(learner.getId());
        if (!isLearner) {
            log.info("User {} is the mentor; review popup is not applicable for mentor", currentUserId);
            return new CallSessionCompleteResponse(false, targetBooking.getId(), mentorProfile.getId(),
                    mentorName, request.getDurationSeconds(), "Mentor is not eligible to review themselves");
        }

        // Verify that review doesn't already exist for this booking
        boolean alreadyReviewed = reviewRepository.findByBookingId(targetBooking.getId()).isPresent();
        if (alreadyReviewed) {
            log.info("Booking ID {} has already been reviewed", targetBooking.getId());
            return new CallSessionCompleteResponse(false, targetBooking.getId(), mentorProfile.getId(),
                    mentorName, request.getDurationSeconds(), "Session has already been reviewed");
        }

        log.info("Learner user {} is ELIGIBLE to review session for booking ID {} with mentor {}",
                currentUserId, targetBooking.getId(), mentorName);
        return new CallSessionCompleteResponse(true, targetBooking.getId(), mentorProfile.getId(),
                mentorName, request.getDurationSeconds(), "Session completed and eligible for review");
    }
}
