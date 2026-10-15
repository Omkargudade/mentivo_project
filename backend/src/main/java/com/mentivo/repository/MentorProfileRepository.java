package com.mentivo.repository;

import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.enums.OnlineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long>, JpaSpecificationExecutor<MentorProfile> {

    Optional<MentorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<MentorProfile> findByOnlineStatus(OnlineStatus onlineStatus, Pageable pageable);

    long countByOnlineStatus(OnlineStatus onlineStatus);

    @Query("SELECT COUNT(m) FROM MentorProfile m WHERE m.isAcceptingBookings = true")
    long countActiveMentors();
}
