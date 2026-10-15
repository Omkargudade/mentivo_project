package com.mentivo.repository;

import com.mentivo.entity.VerificationRequest;
import com.mentivo.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    Page<VerificationRequest> findByStatus(VerificationStatus status, Pageable pageable);
    List<VerificationRequest> findByMentorIdOrderByCreatedAtDesc(Long mentorId);
    long countByStatus(VerificationStatus status);
}
