package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/features", "/api/admin/features"})
@Tag(name = "Feature Flags", description = "Centralized system feature availability configuration")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    @Operation(summary = "Get active/inactive status for all platform features")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.getFeatures()));
    }

    @PutMapping({"/admin/{featureName}", "/{featureName}"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable a feature (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> updateFeature(
            @PathVariable String featureName,
            @RequestParam(required = false) Boolean enabled,
            @RequestBody(required = false) Map<String, Boolean> body) {
        boolean isEnabled = true;
        if (enabled != null) {
            isEnabled = enabled;
        } else if (body != null && body.containsKey("enabled")) {
            isEnabled = body.get("enabled");
        }
        featureFlagService.updateFeature(featureName, isEnabled);
        return ResponseEntity.ok(ApiResponse.success("Feature flag updated successfully", featureFlagService.getFeatures()));
    }
}
