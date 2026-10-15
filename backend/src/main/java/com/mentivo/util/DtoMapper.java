package com.mentivo.util;

import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.category.CategoryDto;
import com.mentivo.dto.category.CategorySummaryDto;
import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.dto.chat.ConversationDto;
import com.mentivo.dto.chat.MessageDto;
import com.mentivo.dto.connection.ConnectionDto;
import com.mentivo.dto.mentor.MentorAvailabilityDto;
import com.mentivo.dto.mentor.MentorCardDto;
import com.mentivo.dto.mentor.MentorDetailDto;
import com.mentivo.dto.notification.NotificationDto;
import com.mentivo.dto.payment.TransactionDto;
import com.mentivo.dto.report.BlockDto;
import com.mentivo.dto.report.ReportDto;
import com.mentivo.dto.review.ReviewDto;
import com.mentivo.dto.session.MentorSessionDto;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.dto.user.UserSummaryDto;
import com.mentivo.entity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DtoMapper {

    public static UserSummaryDto toUserSummaryDto(User user) {
        if (user == null) return null;
        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setProfileImage(user.getProfileImage());
        dto.setHeadline(user.getHeadline());
        dto.setPrimaryRole(user.getPrimaryRole());
        dto.setCurrentMode(user.getCurrentMode());
        dto.setVerified(user.isVerified());
        dto.setVerificationLevel(user.getVerificationLevel());
        dto.setOnlineStatus(user.getOnlineStatus());
        dto.setLastSeen(user.getLastSeen());
        if (user.getMentorProfile() != null) {
            dto.setMentorProfileId(user.getMentorProfile().getId());
        }
        return dto;
    }

    public static UserProfileDto toUserProfileDto(User user) {
        if (user == null) return null;
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setHeadline(user.getHeadline());
        dto.setBio(user.getBio());
        dto.setProfileImage(user.getProfileImage());
        dto.setLocation(user.getLocation());
        dto.setLanguages(user.getLanguages());
        dto.setPrimaryRole(user.getPrimaryRole());
        dto.setCurrentMode(user.getCurrentMode());
        dto.setEducation(user.getEducation());
        dto.setExperience(user.getExperience());
        dto.setWebsite(user.getWebsite());
        dto.setLinkedIn(user.getLinkedIn());
        dto.setGitHub(user.getGitHub());
        dto.setPublic(user.isPublic());
        dto.setVerified(user.isVerified());
        dto.setVerificationLevel(user.getVerificationLevel());
        dto.setOnlineStatus(user.getOnlineStatus());
        dto.setLastSeen(user.getLastSeen());
        dto.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        if (user.getMentorProfile() != null) {
            dto.setMentorProfileId(user.getMentorProfile().getId());
        }
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setIconName(category.getIconName());
        dto.setDescription(category.getDescription());
        if (category.getTopics() != null) {
            dto.setTopics(category.getTopics().stream().map(DtoMapper::toTopicDto).collect(Collectors.toList()));
        }
        if (category.getSkills() != null) {
            dto.setSkills(category.getSkills().stream().map(DtoMapper::toSkillDto).collect(Collectors.toList()));
        }
        return dto;
    }

    public static SkillDto toSkillDto(Skill skill) {
        if (skill == null) return null;
        SkillDto dto = new SkillDto();
        dto.setId(skill.getId());
        dto.setName(skill.getName());
        if (skill.getCategory() != null) {
            dto.setCategoryId(skill.getCategory().getId());
            dto.setCategoryName(skill.getCategory().getName());
        }
        return dto;
    }

    public static TopicDto toTopicDto(Topic topic) {
        if (topic == null) return null;
        TopicDto dto = new TopicDto();
        dto.setId(topic.getId());
        dto.setName(topic.getName());
        dto.setSlug(topic.getSlug());
        if (topic.getCategory() != null) {
            dto.setCategoryId(topic.getCategory().getId());
            dto.setCategoryName(topic.getCategory().getName());
        }
        return dto;
    }

    public static MentorAvailabilityDto toMentorAvailabilityDto(MentorAvailability availability) {
        if (availability == null) return null;
        MentorAvailabilityDto dto = new MentorAvailabilityDto();
        dto.setId(availability.getId());
        dto.setDayOfWeek(availability.getDayOfWeek());
        dto.setStartTime(availability.getStartTime());
        dto.setEndTime(availability.getEndTime());
        dto.setActive(availability.isActive());
        return dto;
    }

    public static MentorCardDto toMentorCardDto(MentorProfile profile) {
        return toMentorCardDto(profile, null);
    }

    public static MentorCardDto toMentorCardDto(MentorProfile profile, Review latestReview) {
        if (profile == null) return null;
        MentorCardDto dto = new MentorCardDto();
        dto.setId(profile.getId());
        if (profile.getUser() != null) {
            User u = profile.getUser();
            dto.setUserId(u.getId());
            dto.setName(u.getFullName());
            dto.setHeadline(u.getHeadline());
            dto.setBio(u.getBio());
            dto.setProfileImage(u.getProfileImage());
            dto.setLocation(u.getLocation());
            dto.setLanguages(u.getLanguages());
            dto.setVerified(u.isVerified());
            dto.setVerificationLevel(u.getVerificationLevel());
        }
        dto.setHourlyRate(profile.getHourlyRate());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setOnlineStatus(profile.getOnlineStatus());
        dto.setLastSeen(profile.getLastSeen());
        dto.setAverageRating(profile.getAverageRating());
        dto.setTotalReviews(profile.getTotalReviews());
        dto.setSessionsCompleted(profile.getSessionsCompleted());
        dto.setResponseTimeMinutes(profile.getResponseTimeMinutes());

        if (latestReview != null && latestReview.getBooking() != null &&
                latestReview.getBooking().getStatus() == com.mentivo.entity.enums.BookingStatus.COMPLETED) {
            dto.setLatestReviewText(latestReview.getReviewText());
            dto.setLatestReviewRating(latestReview.getRating());
            if (latestReview.getLearner() != null) {
                dto.setLatestReviewLearnerName(latestReview.getLearner().getFullName());
            }
            dto.setLatestReviewVerified(true);
        }

        if (profile.getSkills() != null) {
            dto.setSkills(profile.getSkills().stream().map(DtoMapper::toSkillDto).collect(Collectors.toList()));
        } else {
            dto.setSkills(Collections.emptyList());
        }
        if (profile.getTopics() != null) {
            dto.setTopics(profile.getTopics().stream().map(DtoMapper::toTopicDto).collect(Collectors.toList()));
        } else {
            dto.setTopics(Collections.emptyList());
        }
        dto.setCategories(extractCategories(profile));
        return dto;
    }

    public static MentorDetailDto toMentorDetailDto(MentorProfile profile, List<ReviewDto> recentReviews) {
        if (profile == null) return null;
        MentorDetailDto dto = new MentorDetailDto();
        dto.setId(profile.getId());
        if (profile.getUser() != null) {
            User u = profile.getUser();
            dto.setUserId(u.getId());
            dto.setUser(toUserProfileDto(u));
            dto.setName(u.getFullName());
            dto.setHeadline(u.getHeadline());
            dto.setBio(u.getBio() != null ? u.getBio() : profile.getBioSummary());
            dto.setProfileImage(u.getProfileImage());
            dto.setLocation(u.getLocation());
            dto.setLanguages(u.getLanguages());
            dto.setVerified(u.isVerified());
            dto.setVerificationLevel(u.getVerificationLevel());
        }
        dto.setHourlyRate(profile.getHourlyRate());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setOnlineStatus(profile.getOnlineStatus());
        dto.setLastSeen(profile.getLastSeen());
        dto.setAverageRating(profile.getAverageRating());
        dto.setTotalReviews(profile.getTotalReviews());
        dto.setSessionsCompleted(profile.getSessionsCompleted());
        dto.setResponseTimeMinutes(profile.getResponseTimeMinutes());
        dto.setBioSummary(profile.getBioSummary());
        dto.setAcceptingBookings(profile.isAcceptingBookings());

        if (profile.getSkills() != null) {
            dto.setSkills(profile.getSkills().stream().map(DtoMapper::toSkillDto).collect(Collectors.toList()));
        }
        if (profile.getTopics() != null) {
            dto.setTopics(profile.getTopics().stream().map(DtoMapper::toTopicDto).collect(Collectors.toList()));
        }
        if (profile.getAvailabilities() != null) {
            dto.setAvailabilities(profile.getAvailabilities().stream()
                    .map(DtoMapper::toMentorAvailabilityDto)
                    .collect(Collectors.toList()));
        }
        dto.setRecentReviews(recentReviews != null ? recentReviews : Collections.emptyList());

        dto.setYoutubeUrl(profile.getYoutubeUrl());
        dto.setInstagramUrl(profile.getInstagramUrl());
        dto.setShowYoutubeSubscribers(profile.getShowYoutubeSubscribers());
        dto.setYoutubeSubscribersCount(profile.getYoutubeSubscribersCount());
        dto.setYoutubeSubscribersSource(profile.getYoutubeSubscribersSource());
        dto.setShowInstagramFollowers(profile.getShowInstagramFollowers());
        dto.setInstagramFollowersCount(profile.getInstagramFollowersCount());
        dto.setInstagramFollowersSource(profile.getInstagramFollowersSource());

        dto.setCategories(extractCategories(profile));
        return dto;
    }

    public static MentorDetailDto toMentorDetailDto(MentorProfile profile, List<ReviewDto> recentReviews, long followerCount, boolean isFollowing) {
        MentorDetailDto dto = toMentorDetailDto(profile, recentReviews);
        if (dto != null) {
            dto.setLearnerFollowerCount(followerCount);
            dto.setFollowing(isFollowing);
        }
        return dto;
    }

    public static ConnectionDto toConnectionDto(Connection connection) {
        if (connection == null) return null;
        ConnectionDto dto = new ConnectionDto();
        dto.setId(connection.getId());
        dto.setSender(toUserSummaryDto(connection.getSender()));
        dto.setReceiver(toUserSummaryDto(connection.getReceiver()));
        dto.setMessage(connection.getMessage());
        dto.setStatus(connection.getStatus());
        dto.setCreatedAt(connection.getCreatedAt());
        dto.setUpdatedAt(connection.getUpdatedAt());
        return dto;
    }

    public static ConversationDto toConversationDto(Conversation conversation, Long currentUserId, long unreadCount) {
        if (conversation == null) return null;
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        User peer = conversation.getUserOne().getId().equals(currentUserId)
                ? conversation.getUserTwo()
                : conversation.getUserOne();
        dto.setPeerUser(toUserSummaryDto(peer));
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setUnreadCount(unreadCount);
        return dto;
    }

    public static MessageDto toMessageDto(Message message) {
        if (message == null) return null;
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setRecipientId(message.getRecipient().getId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setReadStatus(message.getReadStatus());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    public static BookingDto toBookingDto(Booking booking) {
        if (booking == null) return null;
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setBookingRef(booking.getBookingRef());
        dto.setLearner(toUserSummaryDto(booking.getLearner()));
        if (booking.getLearner() != null) {
            dto.setLearnerUserId(booking.getLearner().getId());
        }
        dto.setMentor(toMentorCardDto(booking.getMentor()));
        if (booking.getMentor() != null && booking.getMentor().getUser() != null) {
            dto.setMentorUserId(booking.getMentor().getUser().getId());
        }
        dto.setTopic(toTopicDto(booking.getTopic()));
        if (booking.getMentorSession() != null) {
            dto.setSessionId(booking.getMentorSession().getId());
            dto.setSessionTitle(booking.getMentorSession().getTitle());
        } else if (booking.getTopic() != null && booking.getTopic().getName() != null && !booking.getTopic().getName().trim().isEmpty()) {
            dto.setSessionTitle(booking.getTopic().getName());
        } else if (booking.getMeetingNotes() != null && !booking.getMeetingNotes().trim().isEmpty()) {
            dto.setSessionTitle(booking.getMeetingNotes());
        } else {
            dto.setSessionTitle("1-on-1 Mentorship Session");
        }
        dto.setBookingDate(booking.getBookingDate());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setDurationMinutes(booking.getDurationMinutes());
        dto.setHourlyRate(booking.getHourlyRate());
        dto.setSessionPrice(booking.getSessionPrice());
        dto.setPlatformFee(booking.getPlatformFee());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setMentorEarnings(booking.getMentorEarnings());
        dto.setStatus(booking.getStatus());
        dto.setMeetingUrl(booking.getMeetingUrl());
        dto.setMeetingNotes(booking.getMeetingNotes());
        dto.setCancellationReason(booking.getCancellationReason());
        dto.setHasReview(booking.getReview() != null);
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }

    public static TransactionDto toTransactionDto(Transaction transaction) {
        if (transaction == null) return null;
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        if (transaction.getUser() != null) {
            dto.setUserId(transaction.getUser().getId());
        }
        if (transaction.getBooking() != null) {
            dto.setBookingId(transaction.getBooking().getId());
            dto.setBookingRef(transaction.getBooking().getBookingRef());
        }
        dto.setType(transaction.getType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setStatus(transaction.getStatus());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }

    public static ReviewDto toReviewDto(Review review) {
        if (review == null) return null;
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        if (review.getBooking() != null) {
            dto.setBookingId(review.getBooking().getId());
            dto.setSessionDate(review.getBooking().getBookingDate());
            if (review.getBooking().getTopic() != null) {
                dto.setSessionTitle(review.getBooking().getTopic().getName());
            } else if (review.getBooking().getMeetingNotes() != null && !review.getBooking().getMeetingNotes().isBlank()) {
                dto.setSessionTitle(review.getBooking().getMeetingNotes());
            } else {
                dto.setSessionTitle("Mentorship Session");
            }
            // Verified Review rule: Genuinely completed session booking
            dto.setVerified(review.getBooking().getStatus() == com.mentivo.entity.enums.BookingStatus.COMPLETED);
        } else {
            dto.setVerified(false);
        }
        if (review.getMentor() != null) {
            dto.setMentorId(review.getMentor().getId());
        }
        UserSummaryDto learnerDto = toUserSummaryDto(review.getLearner());
        if (learnerDto != null) {
            // Protect learner privacy: never expose private email address on public reviews
            learnerDto.setEmail(null);
        }
        dto.setLearner(learnerDto);
        dto.setRating(review.getRating());
        dto.setReviewText(review.getReviewText());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    public static NotificationDto toNotificationDto(Notification notification) {
        if (notification == null) return null;
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType());
        dto.setRelatedId(notification.getRelatedId());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    public static ReportDto toReportDto(Report report) {
        if (report == null) return null;
        ReportDto dto = new ReportDto();
        dto.setId(report.getId());
        dto.setReporter(toUserSummaryDto(report.getReporter()));
        dto.setReportedUser(toUserSummaryDto(report.getReportedUser()));
        dto.setReasonCategory(report.getReasonCategory());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setAdminNotes(report.getAdminNotes());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }

    public static BlockDto toBlockDto(Block block) {
        if (block == null) return null;
        BlockDto dto = new BlockDto();
        dto.setId(block.getId());
        dto.setBlockedUser(toUserSummaryDto(block.getBlocked()));
        dto.setCreatedAt(block.getCreatedAt());
        return dto;
    }

    public static CategorySummaryDto toCategorySummaryDto(Category category) {
        if (category == null) return null;
        return new CategorySummaryDto(category.getId(), category.getName(), category.getSlug(), category.getIconName());
    }

    public static List<CategorySummaryDto> extractCategories(MentorProfile profile) {
        if (profile == null) return Collections.emptyList();
        Map<Long, CategorySummaryDto> catMap = new LinkedHashMap<>();
        if (profile.getCategories() != null) {
            for (Category c : profile.getCategories()) {
                if (c != null && c.getId() != null) {
                    catMap.put(c.getId(), toCategorySummaryDto(c));
                }
            }
        }
        if (profile.getSkills() != null) {
            for (Skill s : profile.getSkills()) {
                if (s.getCategory() != null && s.getCategory().getId() != null) {
                    catMap.putIfAbsent(s.getCategory().getId(), toCategorySummaryDto(s.getCategory()));
                }
            }
        }
        if (profile.getTopics() != null) {
            for (Topic t : profile.getTopics()) {
                if (t.getCategory() != null && t.getCategory().getId() != null) {
                    catMap.putIfAbsent(t.getCategory().getId(), toCategorySummaryDto(t.getCategory()));
                }
            }
        }
        return new ArrayList<>(catMap.values());
    }

    public static MentorSessionDto toMentorSessionDto(MentorSession session) {
        if (session == null) return null;
        MentorSessionDto dto = new MentorSessionDto();
        dto.setId(session.getId());
        if (session.getMentor() != null) {
            MentorProfile mp = session.getMentor();
            dto.setMentorId(mp.getId());
            if (mp.getUser() != null) {
                dto.setMentorUserId(mp.getUser().getId());
                dto.setMentorName(mp.getUser().getFullName());
                dto.setMentorHeadline(mp.getUser().getHeadline());
                dto.setMentorProfileImage(mp.getUser().getProfileImage());
                dto.setMentorVerificationLevel(mp.getUser().getVerificationLevel());
            }
        }
        dto.setTitle(session.getTitle());
        dto.setDescription(session.getDescription());
        if (session.getCategory() != null) {
            dto.setCategoryId(session.getCategory().getId());
            dto.setCategoryName(session.getCategory().getName());
            dto.setCategorySlug(session.getCategory().getSlug());
            dto.setCategoryIconName(session.getCategory().getIconName());
        }
        if (session.getTopic() != null) {
            dto.setTopicId(session.getTopic().getId());
            dto.setTopicName(session.getTopic().getName());
        }
        dto.setSessionType(session.getSessionType());
        dto.setSessionDate(session.getSessionDate());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setIsFree(session.getIsFree());
        dto.setPrice(session.getPrice());
        dto.setExternalMeetingUrl(session.getExternalMeetingUrl());
        dto.setStatus(session.getStatus());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }
}
