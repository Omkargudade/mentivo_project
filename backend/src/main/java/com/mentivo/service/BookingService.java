package com.mentivo.service;

import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.booking.BookingPriceCalculationDto;
import com.mentivo.dto.booking.CreateBookingRequest;
import com.mentivo.entity.*;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.*;
import com.mentivo.util.DtoMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSessionRepository mentorSessionRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final BlockRepository blockRepository;
    private final PlatformSettingRepository settingRepository;
    private final NotificationService notificationService;
    private final VideoSessionService videoSessionService;
    private final EmailService emailService;

    @Value("${app.platform.commission-percent:20.0}")
    private Double defaultCommissionPercent = 20.0;

    public BookingService(BookingRepository bookingRepository,
                          MentorProfileRepository mentorProfileRepository,
                          MentorSessionRepository mentorSessionRepository,
                          UserRepository userRepository,
                          TopicRepository topicRepository,
                          BlockRepository blockRepository,
                          PlatformSettingRepository settingRepository,
                          NotificationService notificationService,
                          VideoSessionService videoSessionService,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSessionRepository = mentorSessionRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.blockRepository = blockRepository;
        this.settingRepository = settingRepository;
        this.notificationService = notificationService;
        this.videoSessionService = videoSessionService;
        this.emailService = emailService;
    }

    public Double getActiveCommissionPercent() {
        Double fallback = (defaultCommissionPercent != null) ? defaultCommissionPercent : 20.0;
        return settingRepository.findById("PLATFORM_COMMISSION_PERCENT")
                .map(setting -> {
                    try {
                        return Double.parseDouble(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    public BookingPriceCalculationDto calculatePrice(BigDecimal hourlyRate, int durationMinutes) {
        if (hourlyRate == null || durationMinutes <= 0) {
            throw new BadRequestException("Invalid hourly rate or duration");
        }
        Double commissionPercent = getActiveCommissionPercent();

        BigDecimal durationFraction = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal sessionPrice = hourlyRate.multiply(durationFraction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionFraction = BigDecimal.valueOf(commissionPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal platformFee = sessionPrice.multiply(commissionFraction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mentorEarnings = sessionPrice.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = sessionPrice;

        return new BookingPriceCalculationDto(hourlyRate, durationMinutes, sessionPrice, platformFee, commissionPercent, totalAmount, mentorEarnings);
    }

    @Transactional
    public BookingDto createBooking(Long learnerId, CreateBookingRequest request) {
        if (request == null) {
            throw new BadRequestException("Booking request cannot be null");
        }

        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner not found"));

        MentorSession session = null;
        MentorProfile mentor = null;

        if (request.getSessionId() != null) {
            session = mentorSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + request.getSessionId()));

            if (request.getMentorId() != null) {
                mentor = mentorProfileRepository.findById(request.getMentorId())
                        .or(() -> mentorProfileRepository.findByUserId(request.getMentorId()))
                        .orElse(session.getMentor());
            } else {
                mentor = session.getMentor();
            }

            if (mentor == null) {
                throw new BadRequestException("Session has no associated mentor");
            }

            if (!session.getMentor().getId().equals(mentor.getId())) {
                throw new BadRequestException("Session does not belong to the selected mentor");
            }

            if (session.getStatus() != com.mentivo.entity.enums.SessionStatus.PUBLISHED) {
                throw new BadRequestException("This session is no longer available for booking");
            }

            if (session.getSessionDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Cannot book a past session");
            }

            // Prevent duplicate booking for the same session by this learner
            if (bookingRepository.existsByLearnerIdAndMentorSessionIdAndStatusNotIn(
                    learner.getId(), session.getId(), java.util.List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW))) {
                throw new ConflictException("You have already booked this session.");
            }

            if (request.getBookingDate() == null) {
                request.setBookingDate(session.getSessionDate());
            }
            if (request.getStartTime() == null) {
                request.setStartTime(session.getStartTime());
            }
            if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
                request.setDurationMinutes(session.getDurationMinutes());
            }
            if (request.getTopicId() == null && session.getTopic() != null) {
                request.setTopicId(session.getTopic().getId());
            }
            request.setMentorId(mentor.getId());
        } else {
            if (request.getMentorId() == null) {
                throw new BadRequestException("Mentor ID is required");
            }
            Long mId = request.getMentorId();
            mentor = mentorProfileRepository.findById(mId)
                    .or(() -> mentorProfileRepository.findByUserId(mId))
                    .orElseThrow(() -> new ResourceNotFoundException("Mentor not found with ID: " + mId));
            request.setMentorId(mentor.getId());

            if (request.getBookingDate() == null) {
                throw new BadRequestException("Booking date is required");
            }
            if (request.getStartTime() == null) {
                throw new BadRequestException("Start time is required");
            }
            if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
                throw new BadRequestException("Duration in minutes is required");
            }
        }

        if (request.getDurationMinutes() <= 0 || request.getDurationMinutes() > 480) {
            throw new BadRequestException("Duration must be a positive time up to 480 minutes");
        }

        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past");
        }

        if (learner.getId().equals(mentor.getUser().getId())) {
            throw new BadRequestException("You cannot book a session with yourself");
        }

        if (!mentor.isAcceptingBookings()) {
            throw new BadRequestException("This mentor is currently not accepting new bookings");
        }

        if (blockRepository.isBlockedBetween(learner.getId(), mentor.getUser().getId())) {
            throw new BadRequestException("Unable to book session with this mentor");
        }

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(request.getDurationMinutes());

        if (session == null) {
            // Concurrency-safe double booking conflict validation for custom slots
            List<Booking> conflicts = bookingRepository.findConflictingBookings(
                    mentor.getId(),
                    request.getBookingDate(),
                    startTime,
                    endTime
            );
            if (!conflicts.isEmpty()) {
                throw new ConflictException("The selected time slot conflicts with an existing booking for this mentor.");
            }
        }

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId()).orElse(null);
        } else if (session != null && session.getTopic() != null) {
            topic = session.getTopic();
        }

        BigDecimal hourlyRate;
        BigDecimal sessionPrice;
        BigDecimal platformFee;
        BigDecimal mentorEarnings;
        BigDecimal totalAmount;

        if (session != null) {
            boolean isFree = Boolean.TRUE.equals(session.getIsFree()) ||
                    session.getPrice() == null ||
                    session.getPrice().compareTo(BigDecimal.ZERO) == 0;
            if (isFree) {
                hourlyRate = BigDecimal.ZERO;
                sessionPrice = BigDecimal.ZERO;
                platformFee = BigDecimal.ZERO;
                mentorEarnings = BigDecimal.ZERO;
                totalAmount = BigDecimal.ZERO;
            } else {
                sessionPrice = session.getPrice().setScale(2, RoundingMode.HALF_UP);
                platformFee = sessionPrice.multiply(BigDecimal.valueOf(getActiveCommissionPercent() / 100.0)).setScale(2, RoundingMode.HALF_UP);
                mentorEarnings = sessionPrice.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
                totalAmount = sessionPrice;
                hourlyRate = mentor.getHourlyRate() != null ? mentor.getHourlyRate() : BigDecimal.ZERO;
            }
        } else {
            BookingPriceCalculationDto calc = calculatePrice(mentor.getHourlyRate(), request.getDurationMinutes());
            hourlyRate = calc.getHourlyRate();
            sessionPrice = calc.getSessionPrice();
            platformFee = calc.getPlatformFee();
            mentorEarnings = calc.getMentorEarnings();
            totalAmount = calc.getTotalAmount();
        }

        Booking booking = new Booking();
        booking.setLearner(learner);
        booking.setMentor(mentor);
        booking.setMentorSession(session);
        booking.setTopic(topic);
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDurationMinutes(request.getDurationMinutes());
        booking.setHourlyRate(hourlyRate);
        booking.setSessionPrice(sessionPrice);
        booking.setPlatformFee(platformFee);
        booking.setMentorEarnings(mentorEarnings);
        booking.setTotalAmount(totalAmount);

        // If session is free (₹0/hr), confirm immediately without requiring payment gateway
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.PENDING);
        }

        String notes = request.getMeetingNotes();
        if ((notes == null || notes.trim().isEmpty()) && session != null) {
            notes = session.getTitle();
        }
        booking.setMeetingNotes(notes);

        Booking saved = bookingRepository.save(booking);

        // Generate encrypted secure room meeting URL
        saved.setMeetingUrl(videoSessionService.generateSessionUrl(saved.getBookingRef()));
        saved = bookingRepository.save(saved);

        String sessionTitle = session != null ? session.getTitle() : (topic != null ? topic.getName() : "Mentorship Session");

        if (saved.getStatus() == BookingStatus.CONFIRMED) {
            notificationService.createNotification(
                    mentor.getUser(),
                    "Session Confirmed",
                    learner.getFullName() + " booked \"" + sessionTitle + "\" on " + request.getBookingDate(),
                    NotificationType.BOOKING_ACCEPTED,
                    saved.getId()
            );
            notificationService.createNotification(
                    learner,
                    "Session Confirmed!",
                    "Your session \"" + sessionTitle + "\" with " + mentor.getUser().getFullName() + " on " + request.getBookingDate() + " is confirmed.",
                    NotificationType.BOOKING_ACCEPTED,
                    saved.getId()
            );

            // Asynchronous email notifications safely caught
            try {
                emailService.sendBookingConfirmationToLearner(
                        learner.getEmail(),
                        learner.getFullName(),
                        mentor.getUser().getFullName(),
                        sessionTitle,
                        saved.getBookingDate().toString(),
                        saved.getStartTime() + " - " + saved.getEndTime()
                );
                emailService.sendBookingAlertToMentor(
                        mentor.getUser().getEmail(),
                        mentor.getUser().getFullName(),
                        learner.getFullName(),
                        sessionTitle,
                        saved.getBookingDate().toString(),
                        saved.getStartTime() + " - " + saved.getEndTime()
                );
            } catch (Exception e) {
                // Email delivery failure must never abort persisted booking
            }
        } else {
            notificationService.createNotification(
                    mentor.getUser(),
                    "New Booking Request",
                    learner.getFullName() + " requested \"" + sessionTitle + "\" on " + request.getBookingDate(),
                    NotificationType.BOOKING_CREATED,
                    saved.getId()
            );

            try {
                emailService.sendBookingAlertToMentor(
                        mentor.getUser().getEmail(),
                        mentor.getUser().getFullName(),
                        learner.getFullName(),
                        sessionTitle,
                        saved.getBookingDate().toString(),
                        saved.getStartTime() + " - " + saved.getEndTime()
                );
            } catch (Exception e) {
                // Email delivery failure must never abort persisted booking
            }
        }

        return DtoMapper.toBookingDto(saved);
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // Ensure user is learner, mentor, or admin
        boolean isLearner = booking.getLearner().getId().equals(userId);
        boolean isMentor = booking.getMentor().getUser().getId().equals(userId);
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && user.getRoles().stream().anyMatch(r -> r.name().equals("ROLE_ADMIN"));

        if (!isLearner && !isMentor && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to view this booking");
        }

        return DtoMapper.toBookingDto(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getLearnerBookings(Long learnerId, Pageable pageable) {
        return bookingRepository.findByLearnerIdOrderByBookingDateDescStartTimeDesc(learnerId, pageable)
                .map(DtoMapper::toBookingDto);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getMentorBookings(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        MentorProfile mentor = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));
        return bookingRepository.findByMentorIdOrderByBookingDateDescStartTimeDesc(mentor.getId(), pageable)
                .map(DtoMapper::toBookingDto);
    }

    @Transactional
    public BookingDto cancelBooking(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        boolean isLearner = booking.getLearner().getId().equals(userId);
        boolean isMentor = booking.getMentor().getUser().getId().equals(userId);
        if (!isLearner && !isMentor) {
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Completed sessions cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : "Cancelled by user");
        Booking updated = bookingRepository.save(booking);

        User notifyTarget = isLearner ? booking.getMentor().getUser() : booking.getLearner();
        notificationService.createNotification(
                notifyTarget,
                "Booking Cancelled",
                "Session " + booking.getBookingRef() + " has been cancelled.",
                NotificationType.BOOKING_ACCEPTED,
                booking.getId()
        );

        return DtoMapper.toBookingDto(updated);
    }

    @Transactional
    public BookingDto updateBookingStatus(Long userId, Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        boolean isLearner = booking.getLearner().getId().equals(userId);
        boolean isMentor = booking.getMentor().getUser().getId().equals(userId);
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && user.getRoles().stream().anyMatch(r -> r.name().equals("ROLE_ADMIN"));

        if (!isLearner && !isMentor && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to update this booking");
        }

        if ((newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CONFIRMED) && !isMentor && !isAdmin) {
            throw new UnauthorizedException("Only the mentor or an administrator can mark a session as " + newStatus);
        }

        booking.setStatus(newStatus);
        if (newStatus == BookingStatus.CONFIRMED) {
            String topicName = booking.getTopic() != null ? booking.getTopic().getName() : "Mentorship Session";
            emailService.sendBookingConfirmationToLearner(
                    booking.getLearner().getEmail(),
                    booking.getLearner().getFullName(),
                    booking.getMentor().getUser().getFullName(),
                    topicName,
                    booking.getBookingDate().toString(),
                    booking.getStartTime() + " - " + booking.getEndTime()
            );
        } else if (newStatus == BookingStatus.COMPLETED) {
            MentorProfile profile = booking.getMentor();
            profile.setSessionsCompleted(profile.getSessionsCompleted() + 1);
            mentorProfileRepository.save(profile);

            notificationService.createNotification(
                    booking.getLearner(),
                    "Session Completed",
                    "How was your session with " + profile.getUser().getFullName() + "? Leave a review to help others!",
                    NotificationType.SESSION_COMPLETED,
                    booking.getId()
            );
        }

        Booking updated = bookingRepository.save(booking);
        return DtoMapper.toBookingDto(updated);
    }

    @Transactional(readOnly = true)
    public com.mentivo.dto.booking.MentorBookingSummaryDto getMentorBookingSummary(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        MentorProfile mentor = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));
        long mentorId = mentor.getId();

        long totalBookings = bookingRepository.countByMentorId(mentorId);
        long uniqueLearners = bookingRepository.countDistinctLearnersByMentorId(mentorId);
        long upcoming = bookingRepository.countUpcomingByMentorId(mentorId, LocalDate.now(), LocalTime.now());
        long completed = bookingRepository.countByMentorIdAndStatus(mentorId, BookingStatus.COMPLETED);
        Double earnings = bookingRepository.calculateMentorTotalEarnings(mentorId);

        return new com.mentivo.dto.booking.MentorBookingSummaryDto(
                totalBookings,
                uniqueLearners,
                upcoming,
                completed,
                earnings != null ? earnings : 0.0
        );
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getMentorBookingsFiltered(Long userId, String statusStr, String query, Pageable pageable) {
        if (userId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        MentorProfile mentor = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));

        BookingStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                status = BookingStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String search = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        return bookingRepository.findByMentorFiltered(mentor.getId(), status, search, pageable)
                .map(DtoMapper::toBookingDto);
    }
}
