package com.mentivo.service;

import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.booking.MentorBookingSummaryDto;
import com.mentivo.entity.Booking;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.Topic;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private PlatformSettingRepository settingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VideoSessionService videoSessionService;

    @Mock
    private EmailService emailService;

    @Mock
    private MentorSessionRepository mentorSessionRepository;

    @InjectMocks
    private BookingService bookingService;

    private User mentorUser;
    private MentorProfile mentorProfile;
    private User learnerUser;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        mentorUser = new User();
        mentorUser.setId(10L);
        mentorUser.setFullName("Dr. Rahul Sharma");
        mentorUser.setEmail("rahul.sharma@example.com");

        mentorProfile = new MentorProfile();
        mentorProfile.setId(100L);
        mentorProfile.setUser(mentorUser);

        learnerUser = new User();
        learnerUser.setId(20L);
        learnerUser.setFullName("Priya Patel");
        learnerUser.setEmail("priya.patel@example.com");
        learnerUser.setProfileImage("https://example.com/avatar.jpg");

        testBooking = new Booking();
        testBooking.setId(500L);
        testBooking.setBookingRef("MNT-TEST001");
        testBooking.setMentor(mentorProfile);
        testBooking.setLearner(learnerUser);
        testBooking.setBookingDate(LocalDate.now().plusDays(1));
        testBooking.setStartTime(LocalTime.of(10, 0));
        testBooking.setEndTime(LocalTime.of(11, 0));
        testBooking.setDurationMinutes(60);
        testBooking.setHourlyRate(BigDecimal.valueOf(800.0));
        testBooking.setSessionPrice(BigDecimal.valueOf(800.0));
        testBooking.setPlatformFee(BigDecimal.valueOf(160.0));
        testBooking.setTotalAmount(BigDecimal.valueOf(800.0));
        testBooking.setMentorEarnings(BigDecimal.valueOf(640.0));
        testBooking.setStatus(BookingStatus.CONFIRMED);

        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("Spring Boot Microservices");
        testBooking.setTopic(topic);
    }

    @Test
    @DisplayName("getMentorBookingSummary calculates accurate metrics for authenticated mentor")
    void getMentorBookingSummary_Success() {
        when(mentorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(mentorProfile));
        when(bookingRepository.countByMentorId(100L)).thenReturn(15L);
        when(bookingRepository.countDistinctLearnersByMentorId(100L)).thenReturn(8L);
        when(bookingRepository.countUpcomingByMentorId(eq(100L), any(LocalDate.class), any(LocalTime.class))).thenReturn(4L);
        when(bookingRepository.countByMentorIdAndStatus(100L, BookingStatus.COMPLETED)).thenReturn(10L);
        when(bookingRepository.calculateMentorTotalEarnings(100L)).thenReturn(6400.0);

        MentorBookingSummaryDto summary = bookingService.getMentorBookingSummary(10L);

        assertNotNull(summary);
        assertEquals(15L, summary.getTotalBookings());
        assertEquals(8L, summary.getUniqueLearners());
        assertEquals(4L, summary.getUpcomingSessions());
        assertEquals(10L, summary.getCompletedSessions());
        assertEquals(6400.0, summary.getTotalEarnings());

        verify(bookingRepository).countByMentorId(100L);
        verify(bookingRepository).countDistinctLearnersByMentorId(100L);
        verify(bookingRepository).countByMentorIdAndStatus(100L, BookingStatus.COMPLETED);
    }

    @Test
    @DisplayName("getMentorBookingSummary throws UnauthorizedException when userId is null")
    void getMentorBookingSummary_Unauthenticated() {
        assertThrows(UnauthorizedException.class, () -> bookingService.getMentorBookingSummary(null));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("getMentorBookingSummary throws ResourceNotFoundException when profile not found")
    void getMentorBookingSummary_ProfileNotFound() {
        when(mentorProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getMentorBookingSummary(999L));
    }

    @Test
    @DisplayName("getMentorBookingsFiltered returns booked learners with mapped session title and learner details")
    void getMentorBookingsFiltered_Success() {
        when(mentorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(mentorProfile));
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), PageRequest.of(0, 10), 1);
        when(bookingRepository.findByMentorFiltered(eq(100L), eq(BookingStatus.CONFIRMED), eq("Priya"), any(Pageable.class)))
                .thenReturn(bookingPage);

        Page<BookingDto> result = bookingService.getMentorBookingsFiltered(10L, "CONFIRMED", "Priya", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        BookingDto dto = result.getContent().get(0);
        assertEquals("MNT-TEST001", dto.getBookingRef());
        assertEquals("Priya Patel", dto.getLearner().getFullName());
        assertEquals("priya.patel@example.com", dto.getLearner().getEmail());
        assertEquals("https://example.com/avatar.jpg", dto.getLearner().getProfileImage());
        assertEquals("Spring Boot Microservices", dto.getSessionTitle());
        assertEquals(BookingStatus.CONFIRMED, dto.getStatus());
        assertEquals(LocalDate.now().plusDays(1), dto.getBookingDate());
        assertEquals(LocalTime.of(10, 0), dto.getStartTime());
    }

    @Test
    @DisplayName("getMentorBookingsFiltered defaults session title when topic is null")
    void getMentorBookingsFiltered_NoTopic_DefaultTitle() {
        testBooking.setTopic(null);
        testBooking.setMeetingNotes("Architecture consultation");
        when(mentorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(mentorProfile));
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), PageRequest.of(0, 10), 1);
        when(bookingRepository.findByMentorFiltered(eq(100L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(bookingPage);

        Page<BookingDto> result = bookingService.getMentorBookingsFiltered(10L, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals("Architecture consultation", result.getContent().get(0).getSessionTitle());
    }

    @Test
    @DisplayName("getMentorBookingsFiltered throws UnauthorizedException when userId is null")
    void getMentorBookingsFiltered_Unauthenticated() {
        assertThrows(UnauthorizedException.class,
                () -> bookingService.getMentorBookingsFiltered(null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("updateBookingStatus throws UnauthorizedException when learner attempts to mark session as COMPLETED")
    void updateBookingStatus_LearnerCannotCompleteSession_ThrowsUnauthorizedException() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(20L)).thenReturn(Optional.of(learnerUser));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> bookingService.updateBookingStatus(20L, 500L, BookingStatus.COMPLETED));

        assertTrue(ex.getMessage().contains("Only the mentor or an administrator can mark a session as COMPLETED"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking throws UnauthorizedException when third-party user attempts to cancel booking")
    void cancelBooking_ThirdPartyUser_ThrowsUnauthorizedException() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> bookingService.cancelBooking(999L, 500L, "Cancelled by unauthorized user"));

        assertTrue(ex.getMessage().contains("not authorized to cancel"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking throws ConflictException when learner has already booked the session")
    void createBooking_DuplicateSessionBooking_ThrowsConflictException() {
        com.mentivo.dto.booking.CreateBookingRequest req = new com.mentivo.dto.booking.CreateBookingRequest();
        req.setMentorId(100L);
        req.setSessionId(777L);
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setStartTime(LocalTime.of(15, 0));
        req.setDurationMinutes(60);

        com.mentivo.entity.MentorSession session = new com.mentivo.entity.MentorSession();
        session.setId(777L);
        session.setMentor(mentorProfile);
        session.setStatus(com.mentivo.entity.enums.SessionStatus.PUBLISHED);
        session.setSessionDate(LocalDate.now().plusDays(2));
        session.setStartTime(LocalTime.of(15, 0));
        session.setDurationMinutes(60);

        when(userRepository.findById(20L)).thenReturn(Optional.of(learnerUser));
        when(mentorProfileRepository.findById(100L)).thenReturn(Optional.of(mentorProfile));
        when(mentorSessionRepository.findById(777L)).thenReturn(Optional.of(session));
        when(bookingRepository.existsByLearnerIdAndMentorSessionIdAndStatusNotIn(eq(20L), eq(777L), anyList()))
                .thenReturn(true);

        com.mentivo.exception.ConflictException ex = assertThrows(
                com.mentivo.exception.ConflictException.class,
                () -> bookingService.createBooking(20L, req)
        );

        assertTrue(ex.getMessage().contains("already booked this session"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking with sessionId auto-populates mentor, date, time, duration and confirms free session")
    void createBooking_WithSessionId_AutoPopulatesFields() {
        com.mentivo.dto.booking.CreateBookingRequest req = new com.mentivo.dto.booking.CreateBookingRequest();
        req.setSessionId(888L);
        // Note: mentorId, bookingDate, startTime, durationMinutes are intentionally left null

        com.mentivo.entity.MentorSession session = new com.mentivo.entity.MentorSession();
        session.setId(888L);
        session.setTitle("Free Architecture Review");
        session.setMentor(mentorProfile);
        session.setStatus(com.mentivo.entity.enums.SessionStatus.PUBLISHED);
        session.setSessionDate(LocalDate.now().plusDays(3));
        session.setStartTime(LocalTime.of(16, 0));
        session.setEndTime(LocalTime.of(17, 0));
        session.setDurationMinutes(60);
        session.setIsFree(true);
        session.setPrice(BigDecimal.ZERO);

        when(userRepository.findById(20L)).thenReturn(Optional.of(learnerUser));
        when(mentorSessionRepository.findById(888L)).thenReturn(Optional.of(session));
        when(bookingRepository.existsByLearnerIdAndMentorSessionIdAndStatusNotIn(eq(20L), eq(888L), anyList()))
                .thenReturn(false);
        when(videoSessionService.generateSessionUrl(any())).thenReturn("https://mentivo.com/room/test-123");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(601L);
            return b;
        });

        BookingDto result = bookingService.createBooking(20L, req);

        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(LocalDate.now().plusDays(3), result.getBookingDate());
        assertEquals(LocalTime.of(16, 0), result.getStartTime());
        assertEquals(60, result.getDurationMinutes());
        assertEquals(BigDecimal.ZERO, result.getSessionPrice());
        verify(bookingRepository, atLeastOnce()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking resolves mentor profile when caller passes mentor user ID instead of profile ID")
    void createBooking_ResolvesMentorByUserId() {
        com.mentivo.dto.booking.CreateBookingRequest req = new com.mentivo.dto.booking.CreateBookingRequest();
        req.setMentorId(10L); // Passing mentor User ID (10L), not Profile ID (100L)
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setStartTime(LocalTime.of(14, 0));
        req.setDurationMinutes(60);

        when(userRepository.findById(20L)).thenReturn(Optional.of(learnerUser));
        when(mentorProfileRepository.findById(10L)).thenReturn(Optional.empty());
        when(mentorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(mentorProfile));
        when(bookingRepository.findConflictingBookings(eq(100L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(videoSessionService.generateSessionUrl(any())).thenReturn("https://mentivo.com/room/test-456");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(602L);
            return b;
        });

        BookingDto result = bookingService.createBooking(20L, req);

        assertNotNull(result);
        assertEquals(100L, result.getMentor().getId());
        verify(mentorProfileRepository).findByUserId(10L);
    }
}
