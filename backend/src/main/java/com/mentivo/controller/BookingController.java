package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.booking.BookingPriceCalculationDto;
import com.mentivo.dto.booking.CreateBookingRequest;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings & Sessions", description = "Session booking, scheduling, price calculations, and session lifecycle")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/calculate")
    @Operation(summary = "Calculate exact server-side pricing breakdown preview")
    public ResponseEntity<ApiResponse<BookingPriceCalculationDto>> calculatePrice(
            @RequestParam BigDecimal hourlyRate,
            @RequestParam int durationMinutes) {
        BookingPriceCalculationDto calc = bookingService.calculatePrice(hourlyRate, durationMinutes);
        return ResponseEntity.ok(ApiResponse.success(calc));
    }

    @PostMapping
    @Operation(summary = "Create a new session booking with server-side price calculation and conflict prevention")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto booking = bookingService.createBooking(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session booked successfully. Proceed to payment.", booking));
    }

    @GetMapping
    @Operation(summary = "Get bookings for current user")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getMyBookings(
            @RequestParam(required = false, defaultValue = "learner") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<BookingDto> bookings;
        if ("mentor".equalsIgnoreCase(role)) {
            bookings = bookingService.getMentorBookings(currentUserId, PageRequest.of(page, size));
        } else {
            bookings = bookingService.getLearnerBookings(currentUserId, PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/mentor/summary")
    @Operation(summary = "Get booking statistics summary for mentor dashboard")
    public ResponseEntity<ApiResponse<com.mentivo.dto.booking.MentorBookingSummaryDto>> getMentorBookingSummary() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new com.mentivo.exception.UnauthorizedException("User is not authenticated");
        }
        com.mentivo.dto.booking.MentorBookingSummaryDto summary = bookingService.getMentorBookingSummary(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/mentor/booked-learners")
    @Operation(summary = "Get booked learners with pagination and filters for mentor dashboard")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getBookedLearners(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new com.mentivo.exception.UnauthorizedException("User is not authenticated");
        }
        Page<BookingDto> bookings = bookingService.getMentorBookingsFiltered(currentUserId, status, query, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/mentor")
    @Operation(summary = "Get bookings for mentor mode with optional status and search query filters")
    public ResponseEntity<ApiResponse<java.util.List<BookingDto>>> getMentorBookingsList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new com.mentivo.exception.UnauthorizedException("User is not authenticated");
        }
        Page<BookingDto> bookings = bookingService.getMentorBookingsFiltered(currentUserId, status, query, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings.getContent()));
    }

    @GetMapping("/learner")
    @Operation(summary = "Get bookings for learner mode")
    public ResponseEntity<ApiResponse<java.util.List<BookingDto>>> getLearnerBookingsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<BookingDto> bookings = bookingService.getLearnerBookings(currentUserId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings.getContent()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<ApiResponse<BookingDto>> getBookingById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto booking = bookingService.getBookingById(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Cancel a booking session")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("reason") : "Cancelled by user";
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto cancelled = bookingService.cancelBooking(currentUserId, id, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", cancelled));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending booking request")
    public ResponseEntity<ApiResponse<BookingDto>> confirmBooking(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto updated = bookingService.updateBookingStatus(currentUserId, id, BookingStatus.CONFIRMED);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", updated));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Update booking session status (IN_PROGRESS, COMPLETED)")
    public ResponseEntity<ApiResponse<BookingDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        BookingStatus status = BookingStatus.valueOf(body.get("status").toUpperCase());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto updated = bookingService.updateBookingStatus(currentUserId, id, status);
        return ResponseEntity.ok(ApiResponse.success("Session status updated to " + status, updated));
    }
}
