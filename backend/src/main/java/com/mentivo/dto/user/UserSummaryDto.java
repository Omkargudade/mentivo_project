package com.mentivo.dto.user;

import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.VerificationLevel;

import java.time.LocalDateTime;

public class UserSummaryDto {
    private Long id;
    private String fullName;
    private String email;
    private String profileImage;
    private String headline;
    private PrimaryRole primaryRole;
    private CurrentMode currentMode;
    private boolean isVerified;
    private VerificationLevel verificationLevel;
    private Long mentorProfileId;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastSeen;

    public UserSummaryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public PrimaryRole getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(PrimaryRole primaryRole) { this.primaryRole = primaryRole; }

    public CurrentMode getCurrentMode() { return currentMode; }
    public void setCurrentMode(CurrentMode currentMode) { this.currentMode = currentMode; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }

    public Long getMentorProfileId() { return mentorProfileId; }
    public void setMentorProfileId(Long mentorProfileId) { this.mentorProfileId = mentorProfileId; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
