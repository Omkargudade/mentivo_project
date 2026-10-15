package com.mentivo.repository;

import com.mentivo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Transaction> findByBookingId(Long bookingId);
}
