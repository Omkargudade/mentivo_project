package com.mentivo.service;

import com.mentivo.dto.session.MentorSessionDto;
import com.mentivo.dto.session.MentorSessionRequest;
import com.mentivo.entity.Category;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.MentorSession;
import com.mentivo.entity.Topic;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.SessionStatus;
import com.mentivo.entity.enums.SessionType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.CategoryRepository;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.MentorSessionRepository;
import com.mentivo.repository.TopicRepository;
import com.mentivo.util.DtoMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MentorSessionService {

    private final MentorSessionRepository mentorSessionRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;

    public MentorSessionService(MentorSessionRepository mentorSessionRepository,
                                MentorProfileRepository mentorProfileRepository,
                                CategoryRepository categoryRepository,
                                TopicRepository topicRepository) {
        this.mentorSessionRepository = mentorSessionRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public Page<MentorSessionDto> searchSessions(String query,
                                                 Long categoryId,
                                                 Long topicId,
                                                 SessionType sessionType,
                                                 Boolean isFree,
                                                 Boolean upcomingOnly,
                                                 int page,
                                                 int size,
                                                 String sortBy) {
        Sort sort = Sort.by(Sort.Direction.ASC, "sessionDate").and(Sort.by(Sort.Direction.ASC, "startTime"));
        if ("date_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "sessionDate").and(Sort.by(Sort.Direction.DESC, "startTime"));
        } else if ("price_asc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Specification<MentorSession> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Published sessions only
            predicates.add(cb.equal(root.get("status"), SessionStatus.PUBLISHED));

            // Upcoming sessions
            if (upcomingOnly == null || Boolean.TRUE.equals(upcomingOnly)) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sessionDate"), LocalDate.now()));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Topic filter
            if (topicId != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            }

            // Session type filter
            if (sessionType != null) {
                predicates.add(cb.equal(root.get("sessionType"), sessionType));
            }

            // Free vs Paid filter
            if (isFree != null) {
                if (Boolean.TRUE.equals(isFree)) {
                    predicates.add(cb.or(
                        cb.isTrue(root.get("isFree")),
                        cb.equal(root.get("price"), BigDecimal.ZERO)
                    ));
                } else {
                    predicates.add(cb.and(
                        cb.isFalse(root.get("isFree")),
                        cb.greaterThan(root.get("price"), BigDecimal.ZERO)
                    ));
                }
            }

            // Text query
            if (StringUtils.hasText(query)) {
                String term = "%" + query.toLowerCase().trim() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), term);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), term);

                Join<MentorSession, Category> catJoin = root.join("category", JoinType.LEFT);
                Predicate catMatch = cb.like(cb.lower(catJoin.get("name")), term);

                Join<MentorSession, Topic> topJoin = root.join("topic", JoinType.LEFT);
                Predicate topMatch = cb.like(cb.lower(topJoin.get("name")), term);

                Join<MentorSession, MentorProfile> mentorJoin = root.join("mentor", JoinType.LEFT);
                Join<MentorProfile, User> userJoin = mentorJoin.join("user", JoinType.LEFT);
                Predicate mentorNameMatch = cb.like(cb.lower(userJoin.get("fullName")), term);

                predicates.add(cb.or(titleMatch, descMatch, catMatch, topMatch, mentorNameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MentorSession> resultPage = mentorSessionRepository.findAll(spec, pageable);
        return resultPage.map(DtoMapper::toMentorSessionDto);
    }

    @Transactional(readOnly = true)
    public MentorSessionDto getSessionById(Long id) {
        MentorSession session = mentorSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        return DtoMapper.toMentorSessionDto(session);
    }

    @Transactional(readOnly = true)
    public List<MentorSessionDto> getMySessions(Long userId) {
        List<MentorSession> sessions = mentorSessionRepository.findByMentorUserIdOrderBySessionDateDescStartTimeDesc(userId);
        return sessions.stream().map(DtoMapper::toMentorSessionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MentorSessionDto> getMentorSessions(Long mentorId) {
        List<MentorSession> sessions = mentorSessionRepository.findByMentorIdAndStatusNotOrderBySessionDateDescStartTimeDesc(
                mentorId, SessionStatus.CANCELLED);
        return sessions.stream().map(DtoMapper::toMentorSessionDto).toList();
    }

    @Transactional
    public MentorSessionDto createSession(Long userId, MentorSessionRequest request) {
        MentorProfile mentor = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only registered mentors can create sessions."));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElse(null);
        }

        if (request.getSessionDate() == null || request.getSessionDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Session date cannot be in the past");
        }
        if (request.getStartTime() == null) {
            throw new BadRequestException("Start time is required");
        }
        if (request.getDurationMinutes() != null && (request.getDurationMinutes() < 15 || request.getDurationMinutes() > 480)) {
            throw new BadRequestException("Duration must be between 15 and 480 minutes");
        }

        MentorSession session = new MentorSession();
        session.setMentor(mentor);
        session.setTitle(request.getTitle().trim());
        session.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        session.setCategory(category);
        session.setTopic(topic);
        session.setSessionType(request.getSessionType() != null ? request.getSessionType() : SessionType.INDIVIDUAL_SESSION);
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());

        int duration = request.getDurationMinutes() != null && request.getDurationMinutes() > 0 ? request.getDurationMinutes() : 60;
        session.setDurationMinutes(duration);
        session.setEndTime(request.getEndTime() != null ? request.getEndTime() : request.getStartTime().plusMinutes(duration));

        boolean isFree = Boolean.TRUE.equals(request.getIsFree()) ||
                (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) == 0);
        session.setIsFree(isFree);
        session.setPrice(isFree ? BigDecimal.ZERO : (request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO));

        if (StringUtils.hasText(request.getExternalMeetingUrl())) {
            session.setExternalMeetingUrl(request.getExternalMeetingUrl().trim());
        }

        session.setStatus(SessionStatus.PUBLISHED);

        MentorSession saved = mentorSessionRepository.save(session);
        return DtoMapper.toMentorSessionDto(saved);
    }

    @Transactional
    public MentorSessionDto updateSession(Long userId, Long sessionId, MentorSessionRequest request) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        if (!session.getMentor().getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to update this session.");
        }

        if (request.getTitle() != null) session.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) session.setDescription(request.getDescription().trim());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            session.setCategory(category);
        }

        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId()).orElse(null);
            session.setTopic(topic);
        } else {
            session.setTopic(null);
        }

        if (request.getSessionType() != null) session.setSessionType(request.getSessionType());
        if (request.getSessionDate() != null) {
            if (request.getSessionDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Session date cannot be in the past");
            }
            session.setSessionDate(request.getSessionDate());
        }
        if (request.getStartTime() != null) session.setStartTime(request.getStartTime());

        int duration = request.getDurationMinutes() != null && request.getDurationMinutes() > 0
                ? request.getDurationMinutes()
                : (session.getDurationMinutes() != null ? session.getDurationMinutes() : 60);
        session.setDurationMinutes(duration);

        if (request.getEndTime() != null) {
            session.setEndTime(request.getEndTime());
        } else if (session.getStartTime() != null) {
            session.setEndTime(session.getStartTime().plusMinutes(duration));
        }

        if (request.getIsFree() != null || request.getPrice() != null) {
            boolean isFree = Boolean.TRUE.equals(request.getIsFree()) ||
                    (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) == 0);
            session.setIsFree(isFree);
            session.setPrice(isFree ? BigDecimal.ZERO : (request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO));
        }

        if (request.getExternalMeetingUrl() != null) {
            session.setExternalMeetingUrl(StringUtils.hasText(request.getExternalMeetingUrl()) ? request.getExternalMeetingUrl().trim() : null);
        }

        MentorSession updated = mentorSessionRepository.save(session);
        return DtoMapper.toMentorSessionDto(updated);
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        if (!session.getMentor().getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to delete this session.");
        }

        mentorSessionRepository.delete(session);
    }
}
