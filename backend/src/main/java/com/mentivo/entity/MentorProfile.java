package com.mentivo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentivo.entity.enums.OnlineStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "mentor_profiles", indexes = {
        @Index(name = "idx_mentor_online_status", columnList = "online_status"),
        @Index(name = "idx_mentor_hourly_rate", columnList = "hourly_rate"),
        @Index(name = "idx_mentor_avg_rating", columnList = "average_rating")
})
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.valueOf(300.00);

    @Column(nullable = false)
    private Integer experienceYears = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Column
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer totalReviews = 0;

    @Column(nullable = false)
    private Integer sessionsCompleted = 0;

    @Column(nullable = false)
    private Integer responseTimeMinutes = 15;

    @Column(columnDefinition = "TEXT")
    private String bioSummary;

    @Column(nullable = false)
    private boolean isAcceptingBookings = true;

    @Column(length = 255)
    private String youtubeUrl;

    @Column(length = 255)
    private String instagramUrl;

    @Column(nullable = false)
    private Boolean showYoutubeSubscribers = false;

    @Column
    private Long youtubeSubscribersCount;

    @Column(length = 30)
    private String youtubeSubscribersSource = "SELF_REPORTED";

    @Column(nullable = false)
    private Boolean showInstagramFollowers = false;

    @Column
    private Long instagramFollowersCount;

    @Column(length = 30)
    private String instagramFollowersSource = "SELF_REPORTED";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mentor_skills",
            joinColumns = @JoinColumn(name = "mentor_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mentor_topics",
            joinColumns = @JoinColumn(name = "mentor_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mentor_categories",
            joinColumns = @JoinColumn(name = "mentor_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "mentor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentorAvailability> availabilities = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.lastSeen == null) this.lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public MentorProfile() {}

    public MentorProfile(User user, BigDecimal hourlyRate, Integer experienceYears) {
        this.user = user;
        this.hourlyRate = hourlyRate;
        this.experienceYears = experienceYears;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
        this.lastSeen = LocalDateTime.now();
    }

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

    public Set<Skill> getSkills() { return skills; }
    public void setSkills(Set<Skill> skills) { this.skills = skills; }

    public Set<Topic> getTopics() { return topics; }
    public void setTopics(Set<Topic> topics) { this.topics = topics; }

    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }

    public List<MentorAvailability> getAvailabilities() { return availabilities; }
    public void setAvailabilities(List<MentorAvailability> availabilities) { this.availabilities = availabilities; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public Boolean getShowYoutubeSubscribers() { return showYoutubeSubscribers != null && showYoutubeSubscribers; }
    public void setShowYoutubeSubscribers(Boolean showYoutubeSubscribers) { this.showYoutubeSubscribers = showYoutubeSubscribers; }

    public Long getYoutubeSubscribersCount() { return youtubeSubscribersCount; }
    public void setYoutubeSubscribersCount(Long youtubeSubscribersCount) { this.youtubeSubscribersCount = youtubeSubscribersCount; }

    public String getYoutubeSubscribersSource() { return youtubeSubscribersSource != null ? youtubeSubscribersSource : "SELF_REPORTED"; }
    public void setYoutubeSubscribersSource(String youtubeSubscribersSource) { this.youtubeSubscribersSource = youtubeSubscribersSource; }

    public Boolean getShowInstagramFollowers() { return showInstagramFollowers != null && showInstagramFollowers; }
    public void setShowInstagramFollowers(Boolean showInstagramFollowers) { this.showInstagramFollowers = showInstagramFollowers; }

    public Long getInstagramFollowersCount() { return instagramFollowersCount; }
    public void setInstagramFollowersCount(Long instagramFollowersCount) { this.instagramFollowersCount = instagramFollowersCount; }

    public String getInstagramFollowersSource() { return instagramFollowersSource != null ? instagramFollowersSource : "SELF_REPORTED"; }
    public void setInstagramFollowersSource(String instagramFollowersSource) { this.instagramFollowersSource = instagramFollowersSource; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
