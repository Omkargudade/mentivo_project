package com.mentivo.service;

import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserPresenceService userPresenceService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private MentorProfile testMentorProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Aman Verma");
        testUser.setEmail("aman@example.com");
        testUser.setPrimaryRole(PrimaryRole.MENTOR);
        testUser.setOnlineStatus(OnlineStatus.ONLINE);
        testUser.setProfileImage("/uploads/profile-pictures/old-avatar.jpg");

        testMentorProfile = new MentorProfile();
        testMentorProfile.setId(10L);
        testMentorProfile.setUser(testUser);
        testUser.setMentorProfile(testMentorProfile);
    }

    @Test
    @DisplayName("uploadProfilePicture successfully deletes old image, stores new file, and updates user profile")
    void uploadProfilePicture_Success() {
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileStorageService.storeProfilePicture(newFile)).thenReturn("/uploads/profile-pictures/new-avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileDto result = userService.uploadProfilePicture(1L, newFile);

        assertNotNull(result);
        assertEquals("/uploads/profile-pictures/new-avatar.png", result.getProfileImage());
        assertEquals("/uploads/profile-pictures/new-avatar.png", testUser.getProfileImage());

        verify(fileStorageService).deleteProfilePicture("/uploads/profile-pictures/old-avatar.jpg");
        verify(fileStorageService).storeProfilePicture(newFile);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("uploadProfilePicture throws ResourceNotFoundException if user does not exist")
    void uploadProfilePicture_UserNotFound_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                userService.uploadProfilePicture(999L, file)
        );

        verify(fileStorageService, never()).storeProfilePicture(any());
    }

    @Test
    @DisplayName("deleteProfilePicture deletes physical file, clears profileImage on user, and saves")
    void deleteProfilePicture_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileDto result = userService.deleteProfilePicture(1L);

        assertNotNull(result);
        assertNull(result.getProfileImage());
        assertNull(testUser.getProfileImage());

        verify(fileStorageService).deleteProfilePicture("/uploads/profile-pictures/old-avatar.jpg");
        verify(userRepository).save(testUser);
    }
}
