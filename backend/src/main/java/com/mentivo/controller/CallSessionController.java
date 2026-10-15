package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.call.CallSessionCompleteRequest;
import com.mentivo.dto.call.CallSessionCompleteResponse;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.CallSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/call/session")
@Tag(name = "Call Sessions", description = "Video call session lifecycle and post-call completion")
public class CallSessionController {

    private final CallSessionService callSessionService;

    public CallSessionController(CallSessionService callSessionService) {
        this.callSessionService = callSessionService;
    }

    @PostMapping("/complete")
    @Operation(summary = "Mark a completed video call session and verify review eligibility")
    public ResponseEntity<ApiResponse<CallSessionCompleteResponse>> completeSession(
            @RequestBody CallSessionCompleteRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        CallSessionCompleteResponse response = callSessionService.completeCallSession(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Call session processed", response));
    }
}
