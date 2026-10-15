package com.mentivo.service;

import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.dto.mentor.MentorAvailabilityDto;
import com.mentivo.dto.mentor.MentorCardDto;
import com.mentivo.dto.mentor.MentorDetailDto;
import com.mentivo.dto.mentor.MentorProfileRequest;
import com.mentivo.dto.mentor.MentorSearchFilter;
import com.mentivo.dto.review.ReviewDto;
import com.mentivo.entity.*;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.*;
import com.mentivo.util.DtoMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MentorService {

    private final MentorProfileRepository mentorProfileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final TopicRepository topicRepository;
    private final MentorAvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;
    private final MentorFollowerRepository mentorFollowerRepository;
    private final CategoryRepository categoryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final Map<String, java.time.Instant> offlineContactRateLimits = new java.util.concurrent.ConcurrentHashMap<>();

    public MentorService(MentorProfileRepository mentorProfileRepository,
                         UserRepository userRepository,
                         SkillRepository skillRepository,
                         TopicRepository topicRepository,
                         MentorAvailabilityRepository availabilityRepository,
                         ReviewRepository reviewRepository,
                         MentorFollowerRepository mentorFollowerRepository,
                         CategoryRepository categoryRepository,
                         SimpMessagingTemplate messagingTemplate,
                         UserPresenceService userPresenceService,
                         NotificationService notificationService,
                         EmailService emailService) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.topicRepository = topicRepository;
        this.availabilityRepository = availabilityRepository;
        this.reviewRepository = reviewRepository;
        this.mentorFollowerRepository = mentorFollowerRepository;
        this.categoryRepository = categoryRepository;
        this.messagingTemplate = messagingTemplate;
        this.userPresenceService = userPresenceService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public Page<MentorCardDto> searchMentors(MentorSearchFilter filter) {
        Sort sort = determineSort(filter.getSortBy());
        Pageable pageable = PageRequest.of(Math.max(0, filter.getPage()), Math.max(1, filter.getSize()), sort);

        Specification<MentorProfile> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // Only accepting bookings mentors
            predicates.add(cb.isTrue(root.get("isAcceptingBookings")));

            // Filter by online status
            if (filter.getOnlineStatus() != null) {
                predicates.add(cb.equal(root.get("onlineStatus"), filter.getOnlineStatus()));
            }

            // Price range or Free filter
            if (Boolean.TRUE.equals(filter.getFreeOnly())) {
                predicates.add(cb.equal(root.get("hourlyRate"), BigDecimal.ZERO));
            } else {
                if (filter.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("hourlyRate"), filter.getMinPrice()));
                }
                if (filter.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("hourlyRate"), filter.getMaxPrice()));
                }
            }

            // Rating filter
            if (filter.getMinRating() != null && filter.getMinRating() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), filter.getMinRating()));
            }

            // Experience filter
            if (filter.getMinExperience() != null && filter.getMinExperience() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), filter.getMinExperience()));
            }

            // Join User table
            Join<MentorProfile, User> userJoin = root.join("user", JoinType.INNER);

            // Language filter
            if (StringUtils.hasText(filter.getLanguage())) {
                predicates.add(cb.like(cb.lower(userJoin.get("languages")), "%" + filter.getLanguage().toLowerCase() + "%"));
            }

            // Topic filter
            if (filter.getTopicId() != null) {
                Join<MentorProfile, Topic> topicJoin = root.join("topics", JoinType.INNER);
                predicates.add(cb.equal(topicJoin.get("id"), filter.getTopicId()));
            }

            // Skill filter
            if (filter.getSkillId() != null) {
                Join<MentorProfile, Skill> skillJoin = root.join("skills", JoinType.INNER);
                predicates.add(cb.equal(skillJoin.get("id"), filter.getSkillId()));
            }

            // Category filter (checks direct mentor categories, or topics/skills under this category)
            if (filter.getCategoryId() != null) {
                Join<MentorProfile, Topic> catTopicJoin = root.join("topics", JoinType.LEFT);
                Join<Topic, Category> catJoinFromTopic = catTopicJoin.join("category", JoinType.LEFT);

                Join<MentorProfile, Skill> catSkillJoin = root.join("skills", JoinType.LEFT);
                Join<Skill, Category> catJoinFromSkill = catSkillJoin.join("category", JoinType.LEFT);

                Join<MentorProfile, Category> directCatJoin = root.join("categories", JoinType.LEFT);

                predicates.add(cb.or(
                    cb.equal(catJoinFromTopic.get("id"), filter.getCategoryId()),
                    cb.equal(catJoinFromSkill.get("id"), filter.getCategoryId()),
                    cb.equal(directCatJoin.get("id"), filter.getCategoryId())
                ));
            }

            // Keyword text search (matches name, headline, bio, or skill/topic names)
            if (StringUtils.hasText(filter.getQuery())) {
                String term = "%" + filter.getQuery().toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), term);
                Predicate headlineMatch = cb.like(cb.lower(userJoin.get("headline")), term);
                Predicate bioMatch = cb.like(cb.lower(root.get("bioSummary")), term);

                Join<MentorProfile, Skill> searchSkillJoin = root.join("skills", JoinType.LEFT);
                Predicate skillMatch = cb.like(cb.lower(searchSkillJoin.get("name")), term);

                Join<MentorProfile, Topic> searchTopicJoin = root.join("topics", JoinType.LEFT);
                Predicate topicMatch = cb.like(cb.lower(searchTopicJoin.get("name")), term);

                predicates.add(cb.or(nameMatch, headlineMatch, bioMatch, skillMatch, topicMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MentorProfile> resultPage = mentorProfileRepository.findAll(spec, pageable);
        return resultPage.map(profile -> {
            Review latest = reviewRepository.findByMentorIdOrderByCreatedAtDesc(profile.getId(), PageRequest.of(0, 1))
                    .getContent().stream().findFirst().orElse(null);
            return DtoMapper.toMentorCardDto(profile, latest);
        });
    }

    private Sort determineSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) return Sort.by(Sort.Direction.DESC, "averageRating");
        return switch (sortBy.toLowerCase()) {
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "totalReviews"));
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "hourlyRate");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "hourlyRate");
            case "experience" -> Sort.by(Sort.Direction.DESC, "experienceYears");
            case "online", "available_now" -> Sort.by(Sort.Direction.ASC, "onlineStatus").and(Sort.by(Sort.Direction.DESC, "averageRating"));
            default -> Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "sessionsCompleted"));
        };
    }

    @Transactional(readOnly = true)
    public MentorDetailDto getMentorDetail(Long mentorId, Long currentUserId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with ID: " + mentorId));

        Page<Review> reviewsPage = reviewRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, PageRequest.of(0, 10));
        List<ReviewDto> recentReviews = reviewsPage.getContent().stream()
                .map(DtoMapper::toReviewDto)
                .collect(Collectors.toList());

        long followerCount = mentorFollowerRepository.countByMentorId(mentorId);
        boolean isFollowing = currentUserId != null && mentorFollowerRepository.existsByMentorIdAndLearnerId(mentorId, currentUserId);

        long eligibleReviewCount = reviewRepository.countByMentorId(mentorId);
        Double avgRating = reviewRepository.calculateAverageRatingForMentor(mentorId);

        MentorDetailDto dto = DtoMapper.toMentorDetailDto(profile, recentReviews, followerCount, isFollowing);
        dto.setTotalReviews((int) eligibleReviewCount);
        if (eligibleReviewCount > 0 && avgRating != null) {
            BigDecimal rounded = BigDecimal.valueOf(avgRating).setScale(1, java.math.RoundingMode.HALF_UP);
            dto.setAverageRating(rounded.doubleValue());
        } else {
            dto.setAverageRating(0.0);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public MentorDetailDto getMentorDetail(Long mentorId) {
        return getMentorDetail(mentorId, null);
    }

    @Transactional(readOnly = true)
    public MentorDetailDto getMentorByUserId(Long userId) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found for user ID: " + userId));
        return getMentorDetail(profile.getId(), userId);
    }

    @Transactional
    public MentorDetailDto updateMentorProfile(Long userId, MentorProfileRequest request) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found for user ID: " + userId));

        if (request.getHourlyRate() != null) profile.setHourlyRate(request.getHourlyRate());
        if (request.getExperienceYears() != null) profile.setExperienceYears(request.getExperienceYears());
        if (request.getResponseTimeMinutes() != null) profile.setResponseTimeMinutes(request.getResponseTimeMinutes());
        if (request.getBioSummary() != null) profile.setBioSummary(request.getBioSummary().trim());
        if (request.getAcceptingBookings() != null) profile.setAcceptingBookings(request.getAcceptingBookings());

        // Update social URLs and showcase preferences
        if (request.getYoutubeUrl() != null) {
            profile.setYoutubeUrl(validateSocialUrl(request.getYoutubeUrl(), "YouTube"));
        }
        if (request.getInstagramUrl() != null) {
            profile.setInstagramUrl(validateSocialUrl(request.getInstagramUrl(), "Instagram"));
        }
        if (request.getShowYoutubeSubscribers() != null) {
            profile.setShowYoutubeSubscribers(request.getShowYoutubeSubscribers());
        }
        if (request.getYoutubeSubscribersCount() != null) {
            profile.setYoutubeSubscribersCount(Math.max(0, request.getYoutubeSubscribersCount()));
        }
        if (request.getShowInstagramFollowers() != null) {
            profile.setShowInstagramFollowers(request.getShowInstagramFollowers());
        }
        if (request.getInstagramFollowersCount() != null) {
            profile.setInstagramFollowersCount(Math.max(0, request.getInstagramFollowersCount()));
        }

        // Update skills
        if (request.getSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.getSkillIds()));
            profile.setSkills(skills);
        }

        // Update categories
        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            profile.setCategories(categories);
        }

        // Update topics
        if (request.getTopicIds() != null) {
            Set<Topic> topics = new HashSet<>(topicRepository.findAllById(request.getTopicIds()));
            profile.setTopics(topics);
        }

        // Update availabilities
        if (request.getAvailabilities() != null) {
            availabilityRepository.deleteByMentorId(profile.getId());
            List<MentorAvailability> newAvailabilities = new ArrayList<>();
            for (MentorAvailabilityDto aDto : request.getAvailabilities()) {
                MentorAvailability avail = new MentorAvailability(profile, aDto.getDayOfWeek(), aDto.getStartTime(), aDto.getEndTime());
                avail.setActive(aDto.isActive());
                newAvailabilities.add(avail);
            }
            availabilityRepository.saveAll(newAvailabilities);
            profile.setAvailabilities(newAvailabilities);
        }

        MentorProfile updated = mentorProfileRepository.save(profile);
        return getMentorDetail(updated.getId(), userId);
    }

    private String validateSocialUrl(String url, String platform) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            java.net.URI uri = java.net.URI.create(trimmed);
            String host = uri.getHost();
            if (host == null) {
                throw new BadRequestException("Invalid " + platform + " URL: Malformed web address");
            }
            host = host.toLowerCase();
            if ("YouTube".equalsIgnoreCase(platform)) {
                if (!host.equals("youtube.com") && !host.endsWith(".youtube.com") && !host.equals("youtu.be") && !host.endsWith(".youtu.be")) {
                    throw new BadRequestException("Invalid YouTube URL: Must be a valid YouTube channel or video link (e.g. https://youtube.com/@channel)");
                }
            } else if ("Instagram".equalsIgnoreCase(platform)) {
                if (!host.equals("instagram.com") && !host.endsWith(".instagram.com")) {
                    throw new BadRequestException("Invalid Instagram URL: Must be a valid Instagram profile link (e.g. https://instagram.com/profile)");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + platform + " URL: Malformed web address");
        }
        return trimmed;
    }

    @Transactional
    public Map<String, Object> followMentor(Long mentorId, Long learnerId) {
        if (learnerId == null) {
            throw new BadRequestException("Authentication required to follow a mentor");
        }
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with ID: " + mentorId));

        if (profile.getUser() != null && profile.getUser().getId().equals(learnerId)) {
            throw new BadRequestException("You cannot follow your own mentor profile");
        }

        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner user not found with ID: " + learnerId));

        if (!mentorFollowerRepository.existsByMentorIdAndLearnerId(mentorId, learnerId)) {
            mentorFollowerRepository.save(new MentorFollower(profile, learner));
        }

        long followerCount = mentorFollowerRepository.countByMentorId(mentorId);
        return Map.of(
                "followerCount", followerCount,
                "isFollowing", true,
                "message", "Successfully followed mentor"
        );
    }

    @Transactional
    public Map<String, Object> unfollowMentor(Long mentorId, Long learnerId) {
        if (learnerId == null) {
            throw new BadRequestException("Authentication required to unfollow a mentor");
        }
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ResourceNotFoundException("Mentor profile not found with ID: " + mentorId);
        }

        mentorFollowerRepository.deleteByMentorIdAndLearnerId(mentorId, learnerId);

        long followerCount = mentorFollowerRepository.countByMentorId(mentorId);
        return Map.of(
                "followerCount", followerCount,
                "isFollowing", false,
                "message", "Successfully unfollowed mentor"
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFollowerInfo(Long mentorId, Long currentUserId) {
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ResourceNotFoundException("Mentor profile not found with ID: " + mentorId);
        }
        long followerCount = mentorFollowerRepository.countByMentorId(mentorId);
        boolean isFollowing = currentUserId != null && mentorFollowerRepository.existsByMentorIdAndLearnerId(mentorId, currentUserId);
        return Map.of(
                "followerCount", followerCount,
                "isFollowing", isFollowing
        );
    }

    @Transactional
    public MentorCardDto toggleOnlineStatus(Long userId, OnlineStatus status) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found for user ID: " + userId));

        userPresenceService.setUserPreference(userId, status);
        profile.setOnlineStatus(status);
        profile.setLastSeen(LocalDateTime.now());
        MentorProfile saved = mentorProfileRepository.save(profile);

        if (profile.getUser() != null) {
            User u = profile.getUser();
            u.setOnlineStatus(status);
            u.setLastSeen(LocalDateTime.now());
            userRepository.save(u);
        }

        MentorCardDto card = DtoMapper.toMentorCardDto(saved);

        // Broadcast status update to STOMP topics so learners and contacts see real-time online changes
        try {
            messagingTemplate.convertAndSend("/topic/mentors/status", card);
            messagingTemplate.convertAndSend("/topic/users/status",
                    new com.mentivo.dto.user.UserStatusUpdateDto(card.getUserId(), card.getOnlineStatus(), card.getLastSeen(), card.getId()));
        } catch (Exception ignored) {}

        return card;
    }

    @Transactional
    public Map<String, Object> requestOfflineContact(Long mentorId, Long learnerId, String messageText) {
        MentorProfile mentor = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor not found with ID: " + mentorId));

        if (mentor.getOnlineStatus() == OnlineStatus.ONLINE) {
            throw new BadRequestException("Mentor is currently online. You can connect and chat with them directly in real time!");
        }

        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + learnerId));

        if (mentor.getUser().getId().equals(learner.getId())) {
            throw new BadRequestException("You cannot request offline contact with yourself.");
        }

        // Rate limit: max 1 offline contact per learner-mentor pair every 5 minutes (300 seconds)
        String rateKey = learnerId + "_" + mentorId;
        java.time.Instant lastRequest = offlineContactRateLimits.get(rateKey);
        if (lastRequest != null && java.time.Instant.now().isBefore(lastRequest.plusSeconds(300))) {
            long waitSec = 300 - java.time.Duration.between(lastRequest, java.time.Instant.now()).getSeconds();
            throw new BadRequestException("You recently sent an offline contact request to this mentor. Please wait " + Math.max(1, waitSec) + " seconds before sending another.");
        }
        offlineContactRateLimits.put(rateKey, java.time.Instant.now());

        String preview = (messageText != null && !messageText.trim().isEmpty())
                ? messageText.trim()
                : "A learner requested to connect with you while you were offline.";

        // Save in-app notification for the mentor
        notificationService.createNotification(
                mentor.getUser(),
                "Offline Contact Request",
                learner.getFullName() + " reached out while you were offline: \"" + preview + "\"",
                com.mentivo.entity.enums.NotificationType.OFFLINE_CONTACT_REQUEST,
                learner.getId()
        );

        // Dispatches email asynchronously if SMTP is configured
        boolean emailConfigured = emailService.isEmailConfigured();
        emailService.sendOfflineContactRequest(
                mentor.getUser().getEmail(),
                mentor.getUser().getFullName(),
                learner.getFullName(),
                learner.getEmail(),
                preview
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("emailSent", emailConfigured);
        result.put("mentorEmail", mentor.getUser().getEmail());
        result.put("message", emailConfigured
                ? "Your offline contact request has been emailed to the mentor and saved in their notifications."
                : "Your contact request was saved in the mentor's notifications. Automatic email is not configured yet, so you can also use direct email (mailto:) to reach them.");
        return result;
    }
}
