package com.mentivo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.UserRoleType;
import com.mentivo.entity.enums.VerificationLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_current_mode", columnList = "current_mode")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 150)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String profileImage;

    @Column(length = 100)
    private String location;

    @Column(length = 255)
    private String languages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrimaryRole primaryRole = PrimaryRole.LEARNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurrentMode currentMode = CurrentMode.LEARNER;

    @Column(length = 255)
    private String education;

    @Column(length = 255)
    private String experience;

    @Column(length = 255)
    private String website;

    @Column(length = 255)
    private String linkedIn;

    @Column(length = 255)
    private String gitHub;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationLevel verificationLevel = VerificationLevel.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Column
    private LocalDateTime lastSeen = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private Set<UserRoleType> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MentorProfile mentorProfile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User() {}

    public User(String email, String password, String fullName, PrimaryRole primaryRole) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.primaryRole = primaryRole;
        this.currentMode = (primaryRole == PrimaryRole.MENTOR) ? CurrentMode.MENTOR : CurrentMode.LEARNER;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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

    public PrimaryRole getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(PrimaryRole primaryRole) { this.primaryRole = primaryRole; }

    public CurrentMode getCurrentMode() { return currentMode; }
    public void setCurrentMode(CurrentMode currentMode) { this.currentMode = currentMode; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getLinkedIn() { return linkedIn; }
    public void setLinkedIn(String linkedIn) { this.linkedIn = linkedIn; }

    public String getGitHub() { return gitHub; }
    public void setGitHub(String gitHub) { this.gitHub = gitHub; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }

    public Set<UserRoleType> getRoles() { return roles; }
    public void setRoles(Set<UserRoleType> roles) { this.roles = roles; }

    public MentorProfile getMentorProfile() { return mentorProfile; }
    public void setMentorProfile(MentorProfile mentorProfile) { this.mentorProfile = mentorProfile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
