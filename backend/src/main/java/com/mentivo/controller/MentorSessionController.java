package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.session.MentorSessionDto;
import com.mentivo.dto.session.MentorSessionRequest;
import com.mentivo.entity.enums.SessionType;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.MentorSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Mentor Sessions & Webinars", description = "Public discovery, search, filtering, and mentor management of sessions and webinars")
public class MentorSessionController {

    private final MentorSessionService mentorSessionService;

    public MentorSessionController(MentorSessionService mentorSessionService) {
        this.mentorSessionService = mentorSessionService;
    }

    @GetMapping
    @Operation(summary = "Search and filter mentor sessions and webinars")
    public ResponseEntity<ApiResponse<Page<MentorSessionDto>>> searchSessions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) SessionType sessionType,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false, defaultValue = "true") Boolean upcomingOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "date_asc") String sortBy) {

        Page<MentorSessionDto> results = mentorSessionService.searchSessions(
                query, categoryId, topicId, sessionType, isFree, upcomingOnly, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session details by ID")
    public ResponseEntity<ApiResponse<MentorSessionDto>> getSessionById(@PathVariable Long id) {
        MentorSessionDto session = mentorSessionService.getSessionById(id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @GetMapping("/mentor/{mentorId}")
    @Operation(summary = "Get published sessions by mentor ID")
    public ResponseEntity<ApiResponse<List<MentorSessionDto>>> getMentorSessions(@PathVariable Long mentorId) {
        List<MentorSessionDto> sessions = mentorSessionService.getMentorSessions(mentorId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/mentor/me")
    @Operation(summary = "Get authenticated mentor's sessions")
    public ResponseEntity<ApiResponse<List<MentorSessionDto>>> getMySessions() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to view your sessions.");
        }
        List<MentorSessionDto> sessions = mentorSessionService.getMySessions(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PostMapping
    @Operation(summary = "Publish a new session or webinar (mentor only)")
    public ResponseEntity<ApiResponse<MentorSessionDto>> createSession(@Valid @RequestBody MentorSessionRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to publish sessions.");
        }
        MentorSessionDto created = mentorSessionService.createSession(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Session published successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing session or webinar")
    public ResponseEntity<ApiResponse<MentorSessionDto>> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody MentorSessionRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to update sessions.");
        }
        MentorSessionDto updated = mentorSessionService.updateSession(currentUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Session updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an existing session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to delete sessions.");
        }
        mentorSessionService.deleteSession(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Session deleted successfully", null));
    }
}
