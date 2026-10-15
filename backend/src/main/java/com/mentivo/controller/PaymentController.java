package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.payment.PaymentOrderDto;
import com.mentivo.dto.payment.PaymentVerificationRequest;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments & Transactions", description = "Order creation, checkout initialization, and payment signature verification")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    @Operation(summary = "Initialize payment order for a booking")
    public ResponseEntity<ApiResponse<PaymentOrderDto>> createOrder(@RequestBody Map<String, Long> payload) {
        Long bookingId = payload.get("bookingId");
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PaymentOrderDto order = paymentService.createPaymentOrder(currentUserId, bookingId);
        return ResponseEntity.ok(ApiResponse.success("Payment order created", order));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify cryptographic payment signature and confirm booking")
    public ResponseEntity<ApiResponse<BookingDto>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BookingDto booking = paymentService.verifyPayment(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and booking confirmed!", booking));
    }
}
