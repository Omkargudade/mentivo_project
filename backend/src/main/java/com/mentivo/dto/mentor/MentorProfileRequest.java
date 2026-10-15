package com.mentivo.dto.mentor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class MentorProfileRequest {

    @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
    private BigDecimal hourlyRate;

    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears = 1;

    private Integer responseTimeMinutes = 15;
    private String bioSummary;
    private Boolean isAcceptingBookings = true;
    private Set<Long> categoryIds;
    private Set<Long> skillIds;
    private Set<Long> topicIds;
    private List<MentorAvailabilityDto> availabilities;

    private String youtubeUrl;
    private String instagramUrl;
    private Boolean showYoutubeSubscribers;
    private Long youtubeSubscribersCount;
    private Boolean showInstagramFollowers;
    private Long instagramFollowersCount;

    public MentorProfileRequest() {}

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public Integer getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Integer responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }

    public String getBioSummary() { return bioSummary; }
    public void setBioSummary(String bioSummary) { this.bioSummary = bioSummary; }

    public Boolean getAcceptingBookings() { return isAcceptingBookings; }
    public void setAcceptingBookings(Boolean acceptingBookings) { isAcceptingBookings = acceptingBookings; }

    public Set<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }

    public Set<Long> getSkillIds() { return skillIds; }
    public void setSkillIds(Set<Long> skillIds) { this.skillIds = skillIds; }

    public Set<Long> getTopicIds() { return topicIds; }
    public void setTopicIds(Set<Long> topicIds) { this.topicIds = topicIds; }

    public List<MentorAvailabilityDto> getAvailabilities() { return availabilities; }
    public void setAvailabilities(List<MentorAvailabilityDto> availabilities) { this.availabilities = availabilities; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public Boolean getShowYoutubeSubscribers() { return showYoutubeSubscribers; }
    public void setShowYoutubeSubscribers(Boolean showYoutubeSubscribers) { this.showYoutubeSubscribers = showYoutubeSubscribers; }

    public Long getYoutubeSubscribersCount() { return youtubeSubscribersCount; }
    public void setYoutubeSubscribersCount(Long youtubeSubscribersCount) { this.youtubeSubscribersCount = youtubeSubscribersCount; }

    public Boolean getShowInstagramFollowers() { return showInstagramFollowers; }
    public void setShowInstagramFollowers(Boolean showInstagramFollowers) { this.showInstagramFollowers = showInstagramFollowers; }

    public Long getInstagramFollowersCount() { return instagramFollowersCount; }
    public void setInstagramFollowersCount(Long instagramFollowersCount) { this.instagramFollowersCount = instagramFollowersCount; }
}
