package com.mentivo.service;

import com.mentivo.dto.auth.*;
import com.mentivo.entity.PasswordResetToken;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.UserRoleType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.PasswordResetTokenRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("learner@example.com");
        testUser.setPassword("encodedOldPassword");
        testUser.setFullName("Test Learner");
        testUser.setPrimaryRole(PrimaryRole.LEARNER);
        testUser.setRoles(Set.of(UserRoleType.ROLE_LEARNER));
        testUser.setVerified(true);
        testUser.setActive(true);
    }

    @Test
    @DisplayName("Registration directly activates account and generates tokens without OTP")
    void register_Success_DirectlyActivatesUserAndGeneratesTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setFullName("New User");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setPrimaryRole(PrimaryRole.LEARNER);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(tokenProvider.generateTokenFromUserId(anyLong(), anyString(), anyString())).thenReturn("mockJwt");
        when(tokenProvider.generateRefreshTokenFromUserId(anyLong())).thenReturn("mockRefreshToken");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertFalse(response.isRequiresVerification());
        assertTrue(response.isVerified());
        assertEquals("mockJwt", response.getToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
    }

    @Test
    @DisplayName("Forgot password generates secure token and sends email")
    void forgotPassword_ExistingUser_GeneratesTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("learner@example.com");

        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("password reset link has been sent"));
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("learner@example.com"), anyString());
    }

    @Test
    @DisplayName("Forgot password with non-existing email returns generic response without error")
    void forgotPassword_UnknownEmail_ReturnsGenericResponseSafely() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        AuthResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("password reset link has been sent"));
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Reset password with valid token updates password and invalidates token")
    void resetPassword_ValidToken_UpdatesPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("rawToken123", "NewSecretPass@123", "NewSecretPass@123");

        PasswordResetToken resetToken = new PasswordResetToken("learner@example.com", "hash", LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByTokenHashAndIsUsedFalse(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewSecretPass@123")).thenReturn("encodedNewSecretPass");

        AuthResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("successfully reset"));
        assertEquals("encodedNewSecretPass", testUser.getPassword());
        assertTrue(resetToken.isUsed());
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).deleteByEmail("learner@example.com");
        verify(emailService).sendPasswordChangedAlert("learner@example.com");
    }

    @Test
    @DisplayName("Reset password with expired token throws BadRequestException")
    void resetPassword_ExpiredToken_ThrowsBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest("rawToken123", "NewSecretPass@123", "NewSecretPass@123");

        PasswordResetToken expiredToken = new PasswordResetToken("learner@example.com", "hash", LocalDateTime.now().minusMinutes(2));

        when(passwordResetTokenRepository.findByTokenHashAndIsUsedFalse(anyString())).thenReturn(Optional.of(expiredToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertTrue(ex.getMessage().contains("expired"));
        verify(userRepository, never()).save(any());
    }
}
