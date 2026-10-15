package com.mentivo.repository;

import com.mentivo.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT r FROM Review r WHERE r.mentor.id = :mentorId AND r.booking.status = com.mentivo.entity.enums.BookingStatus.COMPLETED ORDER BY r.createdAt DESC")
    Page<Review> findByMentorIdOrderByCreatedAtDesc(@Param("mentorId") Long mentorId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mentor.id = :mentorId AND r.booking.status = com.mentivo.entity.enums.BookingStatus.COMPLETED")
    Double calculateAverageRatingForMentor(@Param("mentorId") Long mentorId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.mentor.id = :mentorId AND r.booking.status = com.mentivo.entity.enums.BookingStatus.COMPLETED")
    long countByMentorId(@Param("mentorId") Long mentorId);
}
