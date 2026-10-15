package com.mentivo.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateProfileRequest {
    private String fullName;
    private String headline;
    private String bio;
    private String profileImage;
    private String location;
    private String languages;
    private String education;
    private String experience;
    private String website;
    private String linkedIn;
    private String gitHub;

    @JsonProperty("isPublic")
    private Boolean isPublic;

    public UpdateProfileRequest() {}

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

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Boolean getPublic() { return isPublic; }
    public void setPublic(Boolean aPublic) { this.isPublic = aPublic; }
}
