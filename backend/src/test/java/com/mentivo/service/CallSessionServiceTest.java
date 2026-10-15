package com.mentivo.service;

import com.mentivo.dto.call.CallSessionCompleteRequest;
import com.mentivo.dto.call.CallSessionCompleteResponse;
import com.mentivo.entity.Booking;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.Review;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.ReviewRepository;
import com.mentivo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallSessionServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private CallSessionService callSessionService;

    private User mentorUser;
    private MentorProfile mentorProfile;
    private User learnerUser;
    private Booking booking;

    @BeforeEach
    void setUp() {
        mentorUser = new User();
        mentorUser.setId(10L);
        mentorUser.setFullName("Rahul Sharma");
        mentorUser.setPrimaryRole(PrimaryRole.MENTOR);

        mentorProfile = new MentorProfile();
        mentorProfile.setId(20L);
        mentorProfile.setUser(mentorUser);
        mentorProfile.setSessionsCompleted(2);
        mentorUser.setMentorProfile(mentorProfile);

        learnerUser = new User();
        learnerUser.setId(30L);
        learnerUser.setFullName("Priya Patel");
        learnerUser.setPrimaryRole(PrimaryRole.LEARNER);

        booking = new Booking();
        booking.setId(100L);
        booking.setMentor(mentorProfile);
        booking.setLearner(learnerUser);
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Short call duration under 5 seconds returns not eligible")
    void completeCallSession_ShortDuration_NotEligible() {
        CallSessionCompleteRequest req = new CallSessionCompleteRequest();
        req.setDurationSeconds(3);
        req.setPeerUserId(10L);

        CallSessionCompleteResponse res = callSessionService.completeCallSession(30L, req);

        assertFalse(res.isEligibleForReview());
        assertTrue(res.getMessage().contains("under minimum threshold"));
    }

    @Test
    @DisplayName("Learner completing call is eligible for review and booking status is updated to COMPLETED")
    void completeCallSession_LearnerEligible() {
        when(userRepository.findById(30L)).thenReturn(Optional.of(learnerUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(mentorUser));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByBookingId(100L)).thenReturn(Optional.empty());

        CallSessionCompleteRequest req = new CallSessionCompleteRequest();
        req.setDurationSeconds(120);
        req.setPeerUserId(10L);
        req.setBookingId(100L);

        CallSessionCompleteResponse res = callSessionService.completeCallSession(30L, req);

        assertTrue(res.isEligibleForReview());
        assertEquals(100L, res.getBookingId());
        assertEquals("Rahul Sharma", res.getMentorName());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertEquals(3, mentorProfile.getSessionsCompleted());
        verify(mentorProfileRepository).save(mentorProfile);
    }

    @Test
    @DisplayName("Mentor completing call is marked not eligible for review (mentor cannot review themselves)")
    void completeCallSession_MentorNotEligible() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(mentorUser));
        when(userRepository.findById(30L)).thenReturn(Optional.of(learnerUser));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        CallSessionCompleteRequest req = new CallSessionCompleteRequest();
        req.setDurationSeconds(120);
        req.setPeerUserId(30L);
        req.setBookingId(100L);

        CallSessionCompleteResponse res = callSessionService.completeCallSession(10L, req);

        assertFalse(res.isEligibleForReview());
        assertTrue(res.getMessage().contains("Mentor is not eligible"));
    }

    @Test
    @DisplayName("Already reviewed booking is marked not eligible to prevent duplicate reviews")
    void completeCallSession_AlreadyReviewed_NotEligible() {
        when(userRepository.findById(30L)).thenReturn(Optional.of(learnerUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(mentorUser));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByBookingId(100L)).thenReturn(Optional.of(new Review()));

        CallSessionCompleteRequest req = new CallSessionCompleteRequest();
        req.setDurationSeconds(120);
        req.setPeerUserId(10L);
        req.setBookingId(100L);

        CallSessionCompleteResponse res = callSessionService.completeCallSession(30L, req);

        assertFalse(res.isEligibleForReview());
        assertTrue(res.getMessage().contains("already been reviewed"));
    }
}
