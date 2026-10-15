package com.mentivo.dto.mentor;

import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.dto.review.ReviewDto;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.VerificationLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MentorDetailDto {
    private Long id;
    private Long userId;
    private String name;
    private String headline;
    private String bio;
    private String profileImage;
    private String location;
    private String languages;
    private String gender;
    private UserProfileDto user;
    private BigDecimal hourlyRate;
    private Integer experienceYears;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastSeen;
    private Double averageRating;
    private Integer totalReviews;
    private Integer sessionsCompleted;
    private Integer responseTimeMinutes;
    private String bioSummary;
    private boolean isAcceptingBookings;
    private boolean isVerified;
    private VerificationLevel verificationLevel;
    private List<SkillDto> skills;
    private List<TopicDto> topics;
    private List<com.mentivo.dto.category.CategorySummaryDto> categories;
    private List<MentorAvailabilityDto> availabilities;
    private List<ReviewDto> recentReviews;

    private String youtubeUrl;
    private String instagramUrl;
    private boolean showYoutubeSubscribers;
    private Long youtubeSubscribersCount;
    private String youtubeSubscribersSource;
    private boolean showInstagramFollowers;
    private Long instagramFollowersCount;
    private String instagramFollowersSource;
    private long learnerFollowerCount;
    private boolean isFollowing;

    public MentorDetailDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public UserProfileDto getUser() { return user; }
    public void setUser(UserProfileDto user) { this.user = user; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getSessionsCompleted() { return sessionsCompleted; }
    public void setSessionsCompleted(Integer sessionsCompleted) { this.sessionsCompleted = sessionsCompleted; }

    public Integer getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Integer responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }

    public String getBioSummary() { return bioSummary; }
    public void setBioSummary(String bioSummary) { this.bioSummary = bioSummary; }

    public boolean isAcceptingBookings() { return isAcceptingBookings; }
    public void setAcceptingBookings(boolean acceptingBookings) { isAcceptingBookings = acceptingBookings; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }

    public List<SkillDto> getSkills() { return skills; }
    public void setSkills(List<SkillDto> skills) { this.skills = skills; }

    public List<TopicDto> getTopics() { return topics; }
    public void setTopics(List<TopicDto> topics) { this.topics = topics; }

    public List<MentorAvailabilityDto> getAvailabilities() { return availabilities; }
    public void setAvailabilities(List<MentorAvailabilityDto> availabilities) { this.availabilities = availabilities; }

    public List<ReviewDto> getRecentReviews() { return recentReviews; }
    public void setRecentReviews(List<ReviewDto> recentReviews) { this.recentReviews = recentReviews; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public boolean isShowYoutubeSubscribers() { return showYoutubeSubscribers; }
    public void setShowYoutubeSubscribers(boolean showYoutubeSubscribers) { this.showYoutubeSubscribers = showYoutubeSubscribers; }

    public Long getYoutubeSubscribersCount() { return youtubeSubscribersCount; }
    public void setYoutubeSubscribersCount(Long youtubeSubscribersCount) { this.youtubeSubscribersCount = youtubeSubscribersCount; }

    public String getYoutubeSubscribersSource() { return youtubeSubscribersSource != null ? youtubeSubscribersSource : "SELF_REPORTED"; }
    public void setYoutubeSubscribersSource(String youtubeSubscribersSource) { this.youtubeSubscribersSource = youtubeSubscribersSource; }

    public boolean isShowInstagramFollowers() { return showInstagramFollowers; }
    public void setShowInstagramFollowers(boolean showInstagramFollowers) { this.showInstagramFollowers = showInstagramFollowers; }

    public Long getInstagramFollowersCount() { return instagramFollowersCount; }
    public void setInstagramFollowersCount(Long instagramFollowersCount) { this.instagramFollowersCount = instagramFollowersCount; }

    public String getInstagramFollowersSource() { return instagramFollowersSource != null ? instagramFollowersSource : "SELF_REPORTED"; }
    public void setInstagramFollowersSource(String instagramFollowersSource) { this.instagramFollowersSource = instagramFollowersSource; }

    public long getLearnerFollowerCount() { return learnerFollowerCount; }
    public void setLearnerFollowerCount(long learnerFollowerCount) { this.learnerFollowerCount = learnerFollowerCount; }

    public boolean isFollowing() { return isFollowing; }
    public void setFollowing(boolean following) { isFollowing = following; }

    public List<com.mentivo.dto.category.CategorySummaryDto> getCategories() { return categories; }
    public void setCategories(List<com.mentivo.dto.category.CategorySummaryDto> categories) { this.categories = categories; }
}
