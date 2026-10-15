package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.admin.AdminStatsDto;
import com.mentivo.dto.admin.SettingUpdateDto;
import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.report.ReportDto;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.entity.PlatformSetting;
import com.mentivo.entity.enums.ReportStatus;
import com.mentivo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Portal", description = "Administrative analytics, moderation, and configuration management")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform KPI metrics and statistics")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        AdminStatsDto stats = adminService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "Get paginated list of all users")
    public ResponseEntity<ApiResponse<Page<UserProfileDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserProfileDto> users = adminService.getAllUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/toggle-active")
    @Operation(summary = "Suspend or activate a user account")
    public ResponseEntity<ApiResponse<UserProfileDto>> toggleUserActive(@PathVariable Long id) {
        UserProfileDto user = adminService.toggleUserActive(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated", user));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get moderation reports")
    public ResponseEntity<ApiResponse<Page<ReportDto>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ReportDto> reports = adminService.getReports(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PutMapping("/reports/{id}/resolve")
    @Operation(summary = "Resolve a moderation report")
    public ResponseEntity<ApiResponse<ReportDto>> resolveReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String adminNotes = body.get("adminNotes");
        String statusStr = body.getOrDefault("status", "ACTIONED");
        ReportDto resolved = adminService.resolveReport(id, adminNotes, ReportStatus.valueOf(statusStr.toUpperCase()));
        return ResponseEntity.ok(ApiResponse.success("Report resolved", resolved));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get all platform bookings")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingDto> bookings = adminService.getAllBookings(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get platform settings")
    public ResponseEntity<ApiResponse<List<PlatformSetting>>> getPlatformSettings() {
        List<PlatformSetting> settings = adminService.getPlatformSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update a platform setting")
    public ResponseEntity<ApiResponse<PlatformSetting>> updatePlatformSetting(@Valid @RequestBody SettingUpdateDto dto) {
        PlatformSetting updated = adminService.updatePlatformSetting(dto);
        return ResponseEntity.ok(ApiResponse.success("Platform setting updated", updated));
    }
}
