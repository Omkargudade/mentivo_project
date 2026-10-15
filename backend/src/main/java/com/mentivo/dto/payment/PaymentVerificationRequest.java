package com.mentivo.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentVerificationRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @NotBlank(message = "Payment signature is required")
    private String signature;

    public PaymentVerificationRequest() {}

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
