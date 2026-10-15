package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.user.UpdateProfileRequest;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profiles", description = "User profile retrieval and updates")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto profile = userService.getUserProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto updated = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping(value = "/me/profile-picture", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and set current user's profile picture")
    public ResponseEntity<ApiResponse<UserProfileDto>> uploadProfilePicture(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto updated = userService.updateProfilePicture(currentUserId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated successfully", updated));
    }

    @DeleteMapping("/me/profile-picture")
    @Operation(summary = "Remove current user's profile picture")
    public ResponseEntity<ApiResponse<UserProfileDto>> deleteProfilePicture() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto updated = userService.deleteProfilePicture(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Profile picture removed successfully", updated));
    }

    @RequestMapping(value = "/me/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Toggle user online/offline status (ONLINE / OFFLINE)")
    public ResponseEntity<ApiResponse<UserProfileDto>> toggleUserStatus(@RequestBody Map<String, String> statusBody) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String rawStatus = statusBody.getOrDefault("status", "ONLINE").toUpperCase();
        OnlineStatus status;
        try {
            status = OnlineStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            status = OnlineStatus.ONLINE;
        }
        UserProfileDto updated = userService.toggleUserStatus(currentUserId, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public user profile by ID")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserById(@PathVariable Long id) {
        UserProfileDto profile = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
