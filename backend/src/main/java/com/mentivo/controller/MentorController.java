package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.mentor.MentorCardDto;
import com.mentivo.dto.mentor.MentorDetailDto;
import com.mentivo.dto.mentor.MentorProfileRequest;
import com.mentivo.dto.mentor.MentorSearchFilter;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.MentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/mentors")
@Tag(name = "Mentor Discovery & Profiles", description = "Search, filter, view, and manage mentor profiles")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping
    @Operation(summary = "Search and filter mentors with pagination")
    public ResponseEntity<ApiResponse<Page<MentorCardDto>>> searchMentors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) OnlineStatus onlineStatus,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {

        MentorSearchFilter filter = new MentorSearchFilter();
        filter.setQuery(query);
        filter.setCategoryId(categoryId);
        filter.setTopicId(topicId);
        filter.setSkillId(skillId);
        filter.setLanguage(language);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setFreeOnly(freeOnly);
        filter.setMinRating(minRating);
        filter.setMinExperience(minExperience);
        filter.setOnlineStatus(onlineStatus);
        filter.setSortBy(sortBy);
        filter.setPage(page);
        filter.setSize(size);

        Page<MentorCardDto> mentors = mentorService.searchMentors(filter);
        return ResponseEntity.ok(ApiResponse.success(mentors));
    }

    @GetMapping("/search")
    @Operation(summary = "Alias for mentor search endpoint")
    public ResponseEntity<ApiResponse<Page<MentorCardDto>>> searchMentorsAlias(
            @ModelAttribute MentorSearchFilter filter) {
        Page<MentorCardDto> mentors = mentorService.searchMentors(filter);
        return ResponseEntity.ok(ApiResponse.success(mentors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed mentor profile by ID")
    public ResponseEntity<ApiResponse<MentorDetailDto>> getMentorDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MentorDetailDto mentor = mentorService.getMentorDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(mentor));
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "Follow a mentor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> followMentor(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = mentorService.followMentor(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Successfully followed mentor", result));
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "Unfollow a mentor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfollowMentor(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = mentorService.unfollowMentor(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed mentor", result));
    }

    @PostMapping("/{id}/unfollow")
    @Operation(summary = "Unfollow a mentor (POST alias)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfollowMentorAlias(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = mentorService.unfollowMentor(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed mentor", result));
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "Get mentor website learner follower count and current user follow state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowerInfo(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = mentorService.getFollowerInfo(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated mentor profile")
    public ResponseEntity<ApiResponse<MentorDetailDto>> getMyMentorProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MentorDetailDto profile = mentorService.getMentorByUserId(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current mentor profile, skills, topics, and rates")
    public ResponseEntity<ApiResponse<MentorDetailDto>> updateMentorProfile(@Valid @RequestBody MentorProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MentorDetailDto updated = mentorService.updateMentorProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Mentor profile updated successfully", updated));
    }

    @RequestMapping(value = "/me/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Toggle real online/offline status (ONLINE / OFFLINE)")
    public ResponseEntity<ApiResponse<MentorCardDto>> toggleOnlineStatus(@RequestBody Map<String, String> statusBody) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String rawStatus = statusBody.getOrDefault("status", "ONLINE").toUpperCase();
        OnlineStatus status;
        try {
            status = OnlineStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            status = OnlineStatus.ONLINE;
        }
        MentorCardDto updated = mentorService.toggleOnlineStatus(currentUserId, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
    }

    @PostMapping("/{id}/offline-contact")
    @Operation(summary = "Request contact with a mentor while they are offline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOfflineContact(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String message = (body != null) ? body.get("message") : null;
        Map<String, Object> result = mentorService.requestOfflineContact(id, currentUserId, message);
        return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
    }
}
