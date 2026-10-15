package com.mentivo.dto.auth;

import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.VerificationLevel;

import java.util.Set;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String profileImage;
    private String headline;
    private PrimaryRole primaryRole;
    private CurrentMode currentMode;
    private boolean isVerified;
    private VerificationLevel verificationLevel;
    private Set<String> roles;
    private Long mentorProfileId;
    private boolean requiresVerification = false;
    private String message;

    public AuthResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public Long getMentorProfileId() { return mentorProfileId; }
    public void setMentorProfileId(Long mentorProfileId) { this.mentorProfileId = mentorProfileId; }

    public boolean isRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(boolean requiresVerification) { this.requiresVerification = requiresVerification; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
