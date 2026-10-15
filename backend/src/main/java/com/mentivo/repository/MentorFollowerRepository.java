package com.mentivo.repository;

import com.mentivo.entity.MentorFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentorFollowerRepository extends JpaRepository<MentorFollower, Long> {

    long countByMentorId(Long mentorId);

    boolean existsByMentorIdAndLearnerId(Long mentorId, Long learnerId);

    Optional<MentorFollower> findByMentorIdAndLearnerId(Long mentorId, Long learnerId);

    void deleteByMentorIdAndLearnerId(Long mentorId, Long learnerId);
}
