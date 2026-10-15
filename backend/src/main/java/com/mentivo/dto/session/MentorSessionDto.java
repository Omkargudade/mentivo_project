package com.mentivo.dto.session;

import com.mentivo.entity.enums.SessionStatus;
import com.mentivo.entity.enums.SessionType;
import com.mentivo.entity.enums.VerificationLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MentorSessionDto {
    private Long id;
    private Long mentorId;
    private Long mentorUserId;
    private String mentorName;
    private String mentorHeadline;
    private String mentorProfileImage;
    private VerificationLevel mentorVerificationLevel;

    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String categoryIconName;
    private Long topicId;
    private String topicName;
    private SessionType sessionType;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private Boolean isFree;
    private BigDecimal price;
    private String externalMeetingUrl;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MentorSessionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public Long getMentorUserId() { return mentorUserId; }
    public void setMentorUserId(Long mentorUserId) { this.mentorUserId = mentorUserId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getMentorHeadline() { return mentorHeadline; }
    public void setMentorHeadline(String mentorHeadline) { this.mentorHeadline = mentorHeadline; }

    public String getMentorProfileImage() { return mentorProfileImage; }
    public void setMentorProfileImage(String mentorProfileImage) { this.mentorProfileImage = mentorProfileImage; }

    public VerificationLevel getMentorVerificationLevel() { return mentorVerificationLevel; }
    public void setMentorVerificationLevel(VerificationLevel mentorVerificationLevel) { this.mentorVerificationLevel = mentorVerificationLevel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }

    public String getCategoryIconName() { return categoryIconName; }
    public void setCategoryIconName(String categoryIconName) { this.categoryIconName = categoryIconName; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public SessionType getSessionType() { return sessionType; }
    public void setSessionType(SessionType sessionType) { this.sessionType = sessionType; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Boolean getIsFree() { return isFree != null && isFree; }
    public void setIsFree(Boolean free) { isFree = free; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getExternalMeetingUrl() { return externalMeetingUrl; }
    public void setExternalMeetingUrl(String externalMeetingUrl) { this.externalMeetingUrl = externalMeetingUrl; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
