package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.report.BlockDto;
import com.mentivo.dto.report.BlockRequest;
import com.mentivo.dto.report.CreateReportRequest;
import com.mentivo.dto.report.ReportDto;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.ReportBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Safety & Moderation", description = "Reporting abuse and blocking unwanted users")
public class ReportBlockController {

    private final ReportBlockService reportBlockService;

    public ReportBlockController(ReportBlockService reportBlockService) {
        this.reportBlockService = reportBlockService;
    }

    @PostMapping("/reports")
    @Operation(summary = "Submit a user report (spam, harassment, fraud, inappropriate content, fake profile)")
    public ResponseEntity<ApiResponse<ReportDto>> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReportDto report = reportBlockService.createReport(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thank you for your report. Our safety team will review it.", report));
    }

    @PostMapping("/blocks")
    @Operation(summary = "Block a user from messaging, connecting, or booking")
    public ResponseEntity<ApiResponse<BlockDto>> blockUser(@Valid @RequestBody BlockRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        BlockDto block = reportBlockService.blockUser(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User blocked successfully", block));
    }

    @DeleteMapping("/blocks/{blockedUserId}")
    @Operation(summary = "Unblock a previously blocked user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long blockedUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        reportBlockService.unblockUser(currentUserId, blockedUserId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }
}
