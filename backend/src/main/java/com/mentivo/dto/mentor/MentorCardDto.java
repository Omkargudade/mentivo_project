package com.mentivo.dto.mentor;

import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.VerificationLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MentorCardDto {
    private Long id;
    private Long userId;
    private String name;
    private String headline;
    private String bio;
    private String profileImage;
    private String location;
    private String languages;
    private BigDecimal hourlyRate;
    private Integer experienceYears;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastSeen;
    private Double averageRating;
    private Integer totalReviews;
    private Integer sessionsCompleted;
    private Integer responseTimeMinutes;
    private boolean isVerified;
    private VerificationLevel verificationLevel;
    private List<SkillDto> skills;
    private List<TopicDto> topics;
    private List<com.mentivo.dto.category.CategorySummaryDto> categories;
    private long learnerFollowerCount;

    private String latestReviewText;
    private Integer latestReviewRating;
    private String latestReviewLearnerName;
    private boolean latestReviewVerified;

    public MentorCardDto() {}

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

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }

    public List<SkillDto> getSkills() { return skills; }
    public void setSkills(List<SkillDto> skills) { this.skills = skills; }

    public List<TopicDto> getTopics() { return topics; }
    public void setTopics(List<TopicDto> topics) { this.topics = topics; }

    public long getLearnerFollowerCount() { return learnerFollowerCount; }
    public void setLearnerFollowerCount(long learnerFollowerCount) { this.learnerFollowerCount = learnerFollowerCount; }

    public List<com.mentivo.dto.category.CategorySummaryDto> getCategories() { return categories; }
    public void setCategories(List<com.mentivo.dto.category.CategorySummaryDto> categories) { this.categories = categories; }

    public String getLatestReviewText() { return latestReviewText; }
    public void setLatestReviewText(String latestReviewText) { this.latestReviewText = latestReviewText; }

    public Integer getLatestReviewRating() { return latestReviewRating; }
    public void setLatestReviewRating(Integer latestReviewRating) { this.latestReviewRating = latestReviewRating; }

    public String getLatestReviewLearnerName() { return latestReviewLearnerName; }
    public void setLatestReviewLearnerName(String latestReviewLearnerName) { this.latestReviewLearnerName = latestReviewLearnerName; }

    public boolean isLatestReviewVerified() { return latestReviewVerified; }
    public void setLatestReviewVerified(boolean latestReviewVerified) { this.latestReviewVerified = latestReviewVerified; }
}
