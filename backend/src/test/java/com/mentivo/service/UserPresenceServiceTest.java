package com.mentivo.service;

import com.mentivo.dto.mentor.MentorCardDto;
import com.mentivo.dto.user.UserStatusUpdateDto;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPresenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserPresenceService userPresenceService;

    private User mentorUser;
    private MentorProfile mentorProfile;

    @BeforeEach
    void setUp() {
        mentorUser = new User();
        mentorUser.setId(10L);
        mentorUser.setFullName("Rahul Sharma");
        mentorUser.setEmail("rahul.sharma@example.com");
        mentorUser.setPrimaryRole(PrimaryRole.MENTOR);
        mentorUser.setOnlineStatus(OnlineStatus.OFFLINE);

        mentorProfile = new MentorProfile();
        mentorProfile.setId(20L);
        mentorProfile.setUser(mentorUser);
        mentorProfile.setOnlineStatus(OnlineStatus.OFFLINE);
        mentorProfile.setBioSummary("Senior Engineer");
        mentorUser.setMentorProfile(mentorProfile);
    }

    @Test
    @DisplayName("markUserOnline updates user and mentor profile to ONLINE and broadcasts to both topics")
    void markUserOnline_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> i.getArgument(0));

        userPresenceService.markUserOnline(10L);

        assertEquals(OnlineStatus.ONLINE, mentorUser.getOnlineStatus());
        assertEquals(OnlineStatus.ONLINE, mentorProfile.getOnlineStatus());
        verify(userRepository).save(mentorUser);
        verify(mentorProfileRepository).save(mentorProfile);

        verify(messagingTemplate).convertAndSend(eq("/topic/mentors/status"), any(MentorCardDto.class));
        ArgumentCaptor<UserStatusUpdateDto> userDtoCaptor = ArgumentCaptor.forClass(UserStatusUpdateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/status"), userDtoCaptor.capture());
        assertEquals(OnlineStatus.ONLINE, userDtoCaptor.getValue().getOnlineStatus());
        assertEquals(10L, userDtoCaptor.getValue().getUserId());
        assertEquals(20L, userDtoCaptor.getValue().getMentorProfileId());
    }

    @Test
    @DisplayName("markUserOfflineDueToDisconnect updates user to OFFLINE when currently ONLINE")
    void markUserOfflineDueToDisconnect_Success() {
        mentorUser.setOnlineStatus(OnlineStatus.ONLINE);
        mentorProfile.setOnlineStatus(OnlineStatus.ONLINE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> i.getArgument(0));

        userPresenceService.markUserOfflineDueToDisconnect(10L);

        assertEquals(OnlineStatus.OFFLINE, mentorUser.getOnlineStatus());
        assertEquals(OnlineStatus.OFFLINE, mentorProfile.getOnlineStatus());
        verify(userRepository).save(mentorUser);
        verify(mentorProfileRepository).save(mentorProfile);

        verify(messagingTemplate).convertAndSend(eq("/topic/mentors/status"), any(MentorCardDto.class));
        ArgumentCaptor<UserStatusUpdateDto> userDtoCaptor = ArgumentCaptor.forClass(UserStatusUpdateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/status"), userDtoCaptor.capture());
        assertEquals(OnlineStatus.OFFLINE, userDtoCaptor.getValue().getOnlineStatus());
    }
}
