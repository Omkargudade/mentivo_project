package com.mentivo.service;

import com.mentivo.dto.user.UpdateProfileRequest;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.dto.user.UserStatusUpdateDto;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository,
                       MentorProfileRepository mentorProfileRepository,
                       SimpMessagingTemplate messagingTemplate,
                       UserPresenceService userPresenceService,
                       FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.messagingTemplate = messagingTemplate;
        this.userPresenceService = userPresenceService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return DtoMapper.toUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getFullName() != null) {
            String trimmed = request.getFullName().trim();
            if (trimmed.isEmpty()) {
                throw new BadRequestException("Full name cannot be blank");
            }
            user.setFullName(trimmed);
        }
        if (request.getHeadline() != null) user.setHeadline(request.getHeadline().trim());
        if (request.getBio() != null) {
            String trimmedBio = request.getBio().trim();
            user.setBio(trimmedBio);
            if (user.getMentorProfile() != null) {
                user.getMentorProfile().setBioSummary(trimmedBio);
                mentorProfileRepository.save(user.getMentorProfile());
            }
        }
        if (request.getProfileImage() != null) user.setProfileImage(request.getProfileImage().trim());
        if (request.getLocation() != null) user.setLocation(request.getLocation().trim());
        if (request.getLanguages() != null) user.setLanguages(request.getLanguages().trim());
        if (request.getEducation() != null) user.setEducation(request.getEducation().trim());
        if (request.getExperience() != null) user.setExperience(request.getExperience().trim());
        if (request.getWebsite() != null) user.setWebsite(request.getWebsite().trim());
        if (request.getLinkedIn() != null) user.setLinkedIn(request.getLinkedIn().trim());
        if (request.getGitHub() != null) user.setGitHub(request.getGitHub().trim());
        if (request.getIsPublic() != null) {
            user.setPublic(request.getIsPublic());
        } else if (request.getPublic() != null) {
            user.setPublic(request.getPublic());
        }

        User updatedUser = userRepository.save(user);
        return DtoMapper.toUserProfileDto(updatedUser);
    }

    @Transactional
    public UserProfileDto toggleUserStatus(Long userId, OnlineStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        userPresenceService.setUserPreference(userId, status);
        user.setOnlineStatus(status);
        user.setLastSeen(LocalDateTime.now());

        if (user.getMentorProfile() != null) {
            MentorProfile mp = user.getMentorProfile();
            mp.setOnlineStatus(status);
            mp.setLastSeen(LocalDateTime.now());
            MentorProfile savedMp = mentorProfileRepository.save(mp);
            try {
                messagingTemplate.convertAndSend("/topic/mentors/status", DtoMapper.toMentorCardDto(savedMp));
            } catch (Exception ignored) {}
        }

        User saved = userRepository.save(user);

        // Broadcast to /topic/users/status
        try {
            Long mpId = saved.getMentorProfile() != null ? saved.getMentorProfile().getId() : null;
            messagingTemplate.convertAndSend("/topic/users/status",
                    new UserStatusUpdateDto(saved.getId(), saved.getOnlineStatus(), saved.getLastSeen(), mpId));
        } catch (Exception ignored) {}

        return DtoMapper.toUserProfileDto(saved);
    }

    @Transactional
    public UserProfileDto updateProfilePicture(Long userId, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Delete old uploaded image if it was stored locally
        if (user.getProfileImage() != null) {
            fileStorageService.deleteProfilePicture(user.getProfileImage());
        }

        String newImageUrl = fileStorageService.storeProfilePicture(file);
        user.setProfileImage(newImageUrl);
        User savedUser = userRepository.save(user);

        return DtoMapper.toUserProfileDto(savedUser);
    }

    @Transactional
    public UserProfileDto uploadProfilePicture(Long userId, org.springframework.web.multipart.MultipartFile file) {
        return updateProfilePicture(userId, file);
    }

    @Transactional
    public UserProfileDto deleteProfilePicture(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getProfileImage() != null) {
            fileStorageService.deleteProfilePicture(user.getProfileImage());
        }

        user.setProfileImage(null);
        User savedUser = userRepository.save(user);

        return DtoMapper.toUserProfileDto(savedUser);
    }
}
