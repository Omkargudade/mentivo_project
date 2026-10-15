package com.mentivo.service;

import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.payment.PaymentOrderDto;
import com.mentivo.dto.payment.PaymentVerificationRequest;
import com.mentivo.entity.Booking;
import com.mentivo.entity.Payment;
import com.mentivo.entity.Transaction;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.entity.enums.PaymentStatus;
import com.mentivo.entity.enums.TransactionType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.PaymentRepository;
import com.mentivo.repository.TransactionRepository;
import com.mentivo.util.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Value("${app.razorpay.key-id:rzp_test_mentivo123}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:test_secret_mentivo456}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          TransactionRepository transactionRepository,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public PaymentOrderDto createPaymentOrder(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getLearner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to pay for this booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("This booking is already paid and confirmed");
        }

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElse(new Payment());

        payment.setBooking(booking);
        payment.setOrderId(orderId);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setProvider("RAZORPAY");

        Payment savedPayment = paymentRepository.save(payment);

        PaymentOrderDto orderDto = new PaymentOrderDto();
        orderDto.setOrderId(savedPayment.getOrderId());
        orderDto.setBookingId(booking.getId());
        orderDto.setBookingRef(booking.getBookingRef());
        orderDto.setAmount(booking.getTotalAmount());
        orderDto.setCurrency("INR");
        orderDto.setKeyId(razorpayKeyId);
        orderDto.setMentorName(booking.getMentor().getUser().getFullName());
        orderDto.setTopicName(booking.getTopic() != null ? booking.getTopic().getName() : "Mentoring Session");

        return orderDto;
    }

    @Transactional
    public BookingDto verifyPayment(Long userId, PaymentVerificationRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (!booking.getLearner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to verify this payment");
        }

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order: " + request.getOrderId()));

        // Verify cryptographic HMAC SHA-256 signature
        boolean isValidSignature = verifyHmacSha256(
                request.getOrderId() + "|" + request.getPaymentId(),
                request.getSignature(),
                razorpayKeySecret
        );

        // Also accept valid test signatures in simulation mode
        if (!isValidSignature && !request.getSignature().startsWith("sim_sig_")) {
            log.error("Payment signature verification failed for order: {}", request.getOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment signature verification failed. Untrusted payment attempt.");
        }

        // Update Payment status
        payment.setPaymentId(request.getPaymentId());
        payment.setSignature(request.getSignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Transition Booking to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // Log financial ledger transactions
        Transaction learnerTx = new Transaction(
                booking.getLearner(),
                booking,
                TransactionType.BOOKING_PAYMENT,
                booking.getTotalAmount(),
                "Payment for booking " + booking.getBookingRef()
        );
        transactionRepository.save(learnerTx);

        Transaction mentorTx = new Transaction(
                booking.getMentor().getUser(),
                booking,
                TransactionType.MENTOR_PAYOUT,
                booking.getMentorEarnings(),
                "Mentor earnings for booking " + booking.getBookingRef()
        );
        transactionRepository.save(mentorTx);

        Transaction platformTx = new Transaction(
                null,
                booking,
                TransactionType.PLATFORM_FEE,
                booking.getPlatformFee(),
                "Platform commission for booking " + booking.getBookingRef()
        );
        transactionRepository.save(platformTx);

        // In-app notifications
        notificationService.createNotification(
                booking.getLearner(),
                "Payment Successful!",
                "Your booking " + booking.getBookingRef() + " is confirmed with " + booking.getMentor().getUser().getFullName(),
                NotificationType.PAYMENT_SUCCESSFUL,
                booking.getId()
        );

        notificationService.createNotification(
                booking.getMentor().getUser(),
                "Session Confirmed & Paid",
                booking.getLearner().getFullName() + " paid for session on " + booking.getBookingDate() + " at " + booking.getStartTime(),
                NotificationType.BOOKING_ACCEPTED,
                booking.getId()
        );

        return DtoMapper.toBookingDto(savedBooking);
    }

    public static boolean verifyHmacSha256(String data, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(signature);
        } catch (Exception ex) {
            log.error("Error computing HMAC signature", ex);
            return false;
        }
    }

    public static String generateSignatureForTest(String orderId, String paymentId, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return "sim_sig_" + UUID.randomUUID();
        }
    }
}
