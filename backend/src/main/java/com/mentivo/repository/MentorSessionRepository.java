package com.mentivo.repository;

import com.mentivo.entity.MentorSession;
import com.mentivo.entity.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorSessionRepository extends JpaRepository<MentorSession, Long>, JpaSpecificationExecutor<MentorSession> {

    List<MentorSession> findByMentorIdAndStatusNotOrderBySessionDateDescStartTimeDesc(Long mentorId, SessionStatus status);

    List<MentorSession> findByMentorUserIdAndStatusNotOrderBySessionDateDescStartTimeDesc(Long userId, SessionStatus status);

    List<MentorSession> findByMentorIdOrderBySessionDateDescStartTimeDesc(Long mentorId);

    List<MentorSession> findByMentorUserIdOrderBySessionDateDescStartTimeDesc(Long userId);
}
