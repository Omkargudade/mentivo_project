package com.mentivo.repository;

import com.mentivo.entity.Booking;
import com.mentivo.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingRef(String bookingRef);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.mentor m LEFT JOIN FETCH m.user LEFT JOIN FETCH b.learner LEFT JOIN FETCH b.mentorSession WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    Page<Booking> findByLearnerIdOrderByBookingDateDescStartTimeDesc(Long learnerId, Pageable pageable);

    Page<Booking> findByMentorIdOrderByBookingDateDescStartTimeDesc(Long mentorId, Pageable pageable);

    Page<Booking> findByLearnerIdAndStatusOrderByBookingDateDesc(Long learnerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByMentorIdAndStatusOrderByBookingDateDesc(Long mentorId, BookingStatus status, Pageable pageable);

    List<Booking> findByMentorIdAndBookingDate(Long mentorId, LocalDate bookingDate);

    List<Booking> findByLearnerIdAndMentorId(Long learnerId, Long mentorId);

    boolean existsByLearnerIdAndMentorSessionIdAndStatusNotIn(Long learnerId, Long sessionId, java.util.Collection<BookingStatus> statuses);

    long countByMentorSessionIdAndStatusNotIn(Long sessionId, java.util.Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.mentor.id = :mentorId " +
            "AND b.bookingDate = :date " +
            "AND b.status NOT IN (com.mentivo.entity.enums.BookingStatus.CANCELLED, com.mentivo.entity.enums.BookingStatus.NO_SHOW) " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findConflictingBookings(@Param("mentorId") Long mentorId,
                                         @Param("date") LocalDate date,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime);

    long countByStatus(BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status = com.mentivo.entity.enums.BookingStatus.COMPLETED")
    Double calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(b.platformFee), 0) FROM Booking b WHERE b.status = com.mentivo.entity.enums.BookingStatus.COMPLETED")
    Double calculateTotalPlatformFees();

    @Query("SELECT COALESCE(SUM(b.mentorEarnings), 0) FROM Booking b WHERE b.mentor.id = :mentorId AND b.status = com.mentivo.entity.enums.BookingStatus.COMPLETED")
    Double calculateMentorTotalEarnings(@Param("mentorId") Long mentorId);

    long countByMentorId(Long mentorId);

    long countByMentorIdAndStatus(Long mentorId, BookingStatus status);

    @Query("SELECT COUNT(DISTINCT b.learner.id) FROM Booking b WHERE b.mentor.id = :mentorId")
    long countDistinctLearnersByMentorId(@Param("mentorId") Long mentorId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mentor.id = :mentorId " +
            "AND b.status IN (com.mentivo.entity.enums.BookingStatus.CONFIRMED, com.mentivo.entity.enums.BookingStatus.PENDING) " +
            "AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime >= :nowTime))")
    long countUpcomingByMentorId(@Param("mentorId") Long mentorId,
                                 @Param("today") LocalDate today,
                                 @Param("nowTime") LocalTime nowTime);

    @Query(value = "SELECT b FROM Booking b LEFT JOIN FETCH b.learner LEFT JOIN FETCH b.topic LEFT JOIN FETCH b.mentorSession WHERE b.mentor.id = :mentorId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(b.learner.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.learner.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.bookingRef) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "(b.topic IS NOT NULL AND LOWER(b.topic.name) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
            "(b.mentorSession IS NOT NULL AND LOWER(b.mentorSession.title) LIKE LOWER(CONCAT('%', :query, '%')))) " +
            "ORDER BY b.bookingDate DESC, b.startTime DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b LEFT JOIN b.topic t LEFT JOIN b.mentorSession s WHERE b.mentor.id = :mentorId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(b.learner.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.learner.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.bookingRef) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "(t IS NOT NULL AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
            "(s IS NOT NULL AND LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<Booking> findByMentorFiltered(@Param("mentorId") Long mentorId,
                                       @Param("status") BookingStatus status,
                                       @Param("query") String query,
                                       Pageable pageable);
}
