package com.mentivo.integration;

import com.mentivo.dto.auth.AuthResponse;
import com.mentivo.dto.auth.ForgotPasswordRequest;
import com.mentivo.dto.auth.RegisterRequest;
import com.mentivo.dto.auth.ResetPasswordRequest;
import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.booking.CreateBookingRequest;
import com.mentivo.dto.booking.MentorBookingSummaryDto;
import com.mentivo.dto.session.MentorSessionDto;
import com.mentivo.dto.session.MentorSessionRequest;
import com.mentivo.entity.Booking;
import com.mentivo.entity.Category;
import com.mentivo.entity.PasswordResetToken;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.SessionType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.*;
import com.mentivo.service.AuthService;
import com.mentivo.service.BookingService;
import com.mentivo.service.MentorSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class BookingAndAuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private MentorSessionService mentorSessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MentorProfileRepository mentorProfileRepository;

    @Autowired
    private MentorSessionRepository mentorSessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("End-to-End: Mentor creates session -> Learner books it -> Booking persists in MySQL -> Visible in both dashboards")
    void testEndToEndSessionBookingFlow() {
        // 1. Create and verify a test Mentor
        String mentorEmail = "mentor.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest mentorReq = new RegisterRequest();
        mentorReq.setEmail(mentorEmail);
        mentorReq.setPassword("Password123!");
        mentorReq.setConfirmPassword("Password123!");
        mentorReq.setFullName("Prof. Vikram Rao");
        mentorReq.setPrimaryRole(PrimaryRole.MENTOR);

        AuthResponse mentorAuth = authService.register(mentorReq);
        assertThat(mentorAuth.isRequiresVerification()).isFalse();
        assertThat(mentorAuth.isVerified()).isTrue();

        User mentorUser = userRepository.findByEmail(mentorEmail).orElseThrow();

        // 2. Create and verify a test Learner
        String learnerEmail = "learner.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest learnerReq = new RegisterRequest();
        learnerReq.setEmail(learnerEmail);
        learnerReq.setPassword("Password123!");
        learnerReq.setConfirmPassword("Password123!");
        learnerReq.setFullName("Ananya Gupta");
        learnerReq.setPrimaryRole(PrimaryRole.LEARNER);

        authService.register(learnerReq);
        User learnerUser = userRepository.findByEmail(learnerEmail).orElseThrow();
        learnerUser.setVerified(true);
        learnerUser.setActive(true);
        userRepository.save(learnerUser);

        // 3. Ensure a Category exists for session
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category c = new Category();
            c.setName("Technology");
            c.setSlug("technology");
            return categoryRepository.save(c);
        });

        // 4. Mentor creates a published session
        MentorSessionRequest sessionReq = new MentorSessionRequest();
        sessionReq.setTitle("Microservices Architecture & System Design Deep Dive");
        sessionReq.setDescription("Comprehensive review of distributed patterns and microservices.");
        sessionReq.setCategoryId(category.getId());
        sessionReq.setSessionType(SessionType.INDIVIDUAL_SESSION);
        sessionReq.setSessionDate(LocalDate.now().plusDays(5));
        sessionReq.setStartTime(LocalTime.of(15, 0));
        sessionReq.setDurationMinutes(60);
        sessionReq.setIsFree(true);
        sessionReq.setPrice(BigDecimal.ZERO);

        MentorSessionDto createdSession = mentorSessionService.createSession(mentorUser.getId(), sessionReq);
        assertThat(createdSession).isNotNull();
        assertThat(createdSession.getId()).isNotNull();
        assertThat(createdSession.getTitle()).isEqualTo("Microservices Architecture & System Design Deep Dive");

        // 5. Learner books the session using sessionId (with mentorId, bookingDate, etc. auto-resolved)
        CreateBookingRequest bookingReq = new CreateBookingRequest();
        bookingReq.setSessionId(createdSession.getId());
        bookingReq.setMeetingNotes("Looking forward to discussing API gateways and saga patterns.");

        BookingDto booked = bookingService.createBooking(learnerUser.getId(), bookingReq);
        assertThat(booked).isNotNull();
        assertThat(booked.getId()).isNotNull();
        assertThat(booked.getBookingRef()).isNotNull();
        assertThat(booked.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booked.getSessionTitle()).isEqualTo("Microservices Architecture & System Design Deep Dive");
        assertThat(booked.getBookingDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(booked.getStartTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(booked.getMeetingUrl()).isNotNull();

        // 6. Verify Booking persists in MySQL
        Optional<Booking> persistedInDb = bookingRepository.findById(booked.getId());
        assertThat(persistedInDb).isPresent();
        assertThat(persistedInDb.get().getLearner().getId()).isEqualTo(learnerUser.getId());
        assertThat(persistedInDb.get().getMentor().getUser().getId()).isEqualTo(mentorUser.getId());
        assertThat(persistedInDb.get().getMentorSession().getId()).isEqualTo(createdSession.getId());

        // 7. Verify Booking appears in Learner's Bookings
        Page<BookingDto> learnerBookings = bookingService.getLearnerBookings(learnerUser.getId(), PageRequest.of(0, 10));
        assertThat(learnerBookings.getContent()).anyMatch(b -> b.getId().equals(booked.getId()));

        // 8. Verify Booking appears in Mentor's Dashboard (filtered & summary)
        Page<BookingDto> mentorBookings = bookingService.getMentorBookingsFiltered(mentorUser.getId(), null, null, PageRequest.of(0, 10));
        assertThat(mentorBookings.getContent()).anyMatch(b -> b.getId().equals(booked.getId()) &&
                b.getLearner().getFullName().equals("Ananya Gupta"));

        MentorBookingSummaryDto summary = bookingService.getMentorBookingSummary(mentorUser.getId());
        assertThat(summary.getTotalBookings()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getUniqueLearners()).isGreaterThanOrEqualTo(1);

        // 9. Prevent Duplicate Booking for the same session
        assertThatThrownBy(() -> bookingService.createBooking(learnerUser.getId(), bookingReq))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked this session");

        // 10. Self-booking protection
        CreateBookingRequest selfReq = new CreateBookingRequest();
        selfReq.setSessionId(createdSession.getId());
        assertThatThrownBy(() -> bookingService.createBooking(mentorUser.getId(), selfReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot book a session with yourself");
    }

    @Test
    @DisplayName("Auth & Security: Direct registration without OTP, and single-use password reset tokens")
    void testAuthRegistrationAndResetTokenLifecycle() throws Exception {
        String testEmail = "auth.security." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // 1. Register user -> Account activated directly with tokens and verified = true
        RegisterRequest regReq = new RegisterRequest();
        regReq.setEmail(testEmail);
        regReq.setPassword("SecurePass123!");
        regReq.setConfirmPassword("SecurePass123!");
        regReq.setFullName("Security Tester");
        regReq.setPrimaryRole(PrimaryRole.LEARNER);

        AuthResponse regResp = authService.register(regReq);
        assertThat(regResp.isRequiresVerification()).isFalse();
        assertThat(regResp.isVerified()).isTrue();
        assertThat(regResp.getToken()).isNotNull();

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.isVerified()).isTrue();
        assertThat(user.isActive()).isTrue();

        // 2. Forgot Password -> Creates single-use 256-bit token

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail(testEmail);
        AuthResponse forgotResp = authService.forgotPassword(forgotReq);
        assertThat(forgotResp.getMessage()).contains("password reset link has been sent");

        PasswordResetToken tokenRecord = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equalsIgnoreCase(testEmail))
                .findFirst()
                .orElseThrow();
        assertThat(tokenRecord.getTokenHash()).isNotNull();
        assertThat(tokenRecord.isUsed()).isFalse();

        // Test Reset Password
        // Generate a known token
        byte[] rawBytes = new byte[32];
        for (int i = 0; i < 32; i++) rawBytes[i] = (byte) (i + 1);
        String rawTokenHex = HexFormat.of().formatHex(rawBytes);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String knownHash = HexFormat.of().formatHex(digest.digest(rawTokenHex.getBytes(StandardCharsets.UTF_8)));

        tokenRecord.setTokenHash(knownHash);
        passwordResetTokenRepository.save(tokenRecord);

        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(rawTokenHex);
        resetReq.setNewPassword("BrandNewPass456!");
        resetReq.setConfirmPassword("BrandNewPass456!");

        AuthResponse resetResp = authService.resetPassword(resetReq);
        assertThat(resetResp.getMessage()).contains("successfully reset");

        // Verify password was updated with BCrypt
        User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(passwordEncoder.matches("BrandNewPass456!", updatedUser.getPassword())).isTrue();

        // 4. Token is single-use: Reusing the same token fails
        assertThatThrownBy(() -> authService.resetPassword(resetReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or already used password reset link");
    }
}
