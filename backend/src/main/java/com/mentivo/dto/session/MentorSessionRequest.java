package com.mentivo.dto.session;

import com.mentivo.entity.enums.SessionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class MentorSessionRequest {

    @NotBlank(message = "Session title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Long topicId;

    @NotNull(message = "Session type is required")
    private SessionType sessionType = SessionType.INDIVIDUAL_SESSION;

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    private LocalTime endTime;

    private Integer durationMinutes = 60;

    private Boolean isFree = false;

    private BigDecimal price = BigDecimal.ZERO;

    private String externalMeetingUrl;

    public MentorSessionRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

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
}
