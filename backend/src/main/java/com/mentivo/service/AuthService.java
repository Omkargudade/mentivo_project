package com.mentivo.service;

import com.mentivo.dto.auth.*;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.entity.enums.UserRoleType;
import com.mentivo.entity.enums.VerificationLevel;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.security.JwtTokenProvider;
import com.mentivo.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import com.mentivo.entity.PasswordResetToken;
import com.mentivo.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthService(UserRepository userRepository,
                       MentorProfileRepository mentorProfileRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPrimaryRole(request.getPrimaryRole());
        user.setActive(true);
        user.setVerified(true);
        user.setVerificationLevel(request.getPrimaryRole() == PrimaryRole.MENTOR ? VerificationLevel.SKILL_VERIFIED : VerificationLevel.EMAIL_VERIFIED);

        Set<UserRoleType> roles = new HashSet<>();
        roles.add(UserRoleType.ROLE_LEARNER);

        if (request.getPrimaryRole() == PrimaryRole.MENTOR) {
            roles.add(UserRoleType.ROLE_MENTOR);
            user.setCurrentMode(CurrentMode.MENTOR);
        } else if (request.getPrimaryRole() == PrimaryRole.BOTH) {
            roles.add(UserRoleType.ROLE_MENTOR);
            user.setCurrentMode(CurrentMode.LEARNER);
        } else {
            user.setCurrentMode(CurrentMode.LEARNER);
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Create default mentor profile if user has mentor capability
        MentorProfile mentorProfile = null;
        if (request.getPrimaryRole() == PrimaryRole.MENTOR || request.getPrimaryRole() == PrimaryRole.BOTH) {
            mentorProfile = new MentorProfile(savedUser, BigDecimal.valueOf(300.00), 1);
            mentorProfile.setBioSummary("Passionate about sharing knowledge and mentoring learners.");
            mentorProfile = mentorProfileRepository.save(mentorProfile);
            savedUser.setMentorProfile(mentorProfile);
        }

        String token = tokenProvider.generateTokenFromUserId(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName());
        String refreshToken = tokenProvider.generateRefreshTokenFromUserId(savedUser.getId());
        Long mentorProfileId = mentorProfile != null ? mentorProfile.getId() : null;

        AuthResponse response = buildAuthResponse(savedUser, token, refreshToken, mentorProfileId);
        response.setRequiresVerification(false);
        response.setMessage("Account created successfully! Welcome to Mentivo.");
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadRequestException("Your account has been deactivated. Please contact support.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        Long mentorProfileId = user.getMentorProfile() != null ? user.getMentorProfile().getId() : null;
        AuthResponse response = buildAuthResponse(user, token, refreshToken, mentorProfileId);
        response.setRequiresVerification(false);
        return response;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromJWT(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newToken = tokenProvider.generateTokenFromUserId(user.getId(), user.getEmail(), user.getFullName());
        Long mentorProfileId = user.getMentorProfile() != null ? user.getMentorProfile().getId() : null;

        return buildAuthResponse(user, newToken, request.getRefreshToken(), mentorProfileId);
    }

    @Transactional
    public AuthResponse switchMode(Long userId, ModeSwitchRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getMode() == CurrentMode.MENTOR) {
            // Ensure mentor profile exists
            if (user.getMentorProfile() == null) {
                MentorProfile profile = new MentorProfile(user, BigDecimal.valueOf(300.00), 1);
                profile.setBioSummary("Passionate about sharing knowledge and mentoring learners.");
                profile = mentorProfileRepository.save(profile);
                user.setMentorProfile(profile);
            }
            user.getRoles().add(UserRoleType.ROLE_MENTOR);
            if (user.getPrimaryRole() == PrimaryRole.LEARNER) {
                user.setPrimaryRole(PrimaryRole.BOTH);
            }
        }

        user.setCurrentMode(request.getMode());
        userRepository.save(user);

        String newToken = tokenProvider.generateTokenFromUserId(user.getId(), user.getEmail(), user.getFullName());
        Long mentorProfileId = user.getMentorProfile() != null ? user.getMentorProfile().getId() : null;

        return buildAuthResponse(user, newToken, null, mentorProfileId);
    }

    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().isActive()) {
            User user = userOpt.get();

            // Delete any existing reset tokens for this email
            passwordResetTokenRepository.deleteByEmail(email);

            // Generate cryptographically secure 256-bit random token
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String rawToken = HexFormat.of().formatHex(randomBytes);
            String tokenHash = hashToken(rawToken);

            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
            PasswordResetToken resetToken = new PasswordResetToken(email, tokenHash, expiresAt);
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(email, resetLink);
        }

        // Return a generic response to prevent email enumeration
        AuthResponse response = new AuthResponse();
        response.setEmail(email);
        response.setMessage("If an account exists with this email address, a password reset link has been sent. Please check your inbox.");
        return response;
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation password do not match");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndIsUsedFalse(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or already used password reset link. Please request a new one."));

        if (LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            throw new BadRequestException("Password reset link has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User associated with this reset link not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used immediately
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByEmail(user.getEmail());

        // Send confirmation email
        emailService.sendPasswordChangedAlert(user.getEmail());

        AuthResponse response = new AuthResponse();
        response.setEmail(user.getEmail());
        response.setMessage("Your password has been successfully reset. You may now sign in with your new password.");
        return response;
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken, Long mentorProfileId) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setProfileImage(user.getProfileImage());
        response.setHeadline(user.getHeadline());
        response.setPrimaryRole(user.getPrimaryRole());
        response.setCurrentMode(user.getCurrentMode());
        response.setVerified(user.isVerified());
        response.setVerificationLevel(user.getVerificationLevel());
        response.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        response.setMentorProfileId(mentorProfileId);
        return response;
    }
}
