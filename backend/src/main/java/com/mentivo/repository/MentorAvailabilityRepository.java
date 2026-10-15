package com.mentivo.repository;

import com.mentivo.entity.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {
    List<MentorAvailability> findByMentorId(Long mentorId);
    List<MentorAvailability> findByMentorIdAndIsActiveTrue(Long mentorId);
    List<MentorAvailability> findByMentorIdAndDayOfWeekAndIsActiveTrue(Long mentorId, DayOfWeek dayOfWeek);
    void deleteByMentorId(Long mentorId);
}
