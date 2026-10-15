package com.mentivo.dto.user;

import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.VerificationLevel;

import java.time.LocalDateTime;
import java.util.Set;

public class UserProfileDto {
    private Long id;
    private String email;
    private String fullName;
    private String headline;
    private String bio;
    private String profileImage;
    private String location;
    private String languages;
    private PrimaryRole primaryRole;
    private CurrentMode currentMode;
    private String education;
    private String experience;
    private String website;
    private String linkedIn;
    private String gitHub;
    private boolean isPublic;
    private boolean isVerified;
    private VerificationLevel verificationLevel;
    private Set<String> roles;
    private Long mentorProfileId;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;

    public UserProfileDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public Long getMentorProfileId() { return mentorProfileId; }
    public void setMentorProfileId(Long mentorProfileId) { this.mentorProfileId = mentorProfileId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
