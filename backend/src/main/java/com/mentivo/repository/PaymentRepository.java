package com.mentivo.repository;

import com.mentivo.entity.Payment;
import com.mentivo.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByBookingId(Long bookingId);
    long countByStatus(PaymentStatus status);
}
