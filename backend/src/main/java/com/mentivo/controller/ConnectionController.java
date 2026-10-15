package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.connection.ConnectionDto;
import com.mentivo.dto.connection.CreateConnectionRequest;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connections")
@Tag(name = "Connections", description = "Peer connections between learners and mentors")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping
    @Operation(summary = "Send connection request to a mentor or learner")
    public ResponseEntity<ApiResponse<ConnectionDto>> sendConnectionRequest(
            @Valid @RequestBody CreateConnectionRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConnectionDto connection = connectionService.sendConnectionRequest(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Connection request sent successfully", connection));
    }

    @GetMapping
    @Operation(summary = "Get active accepted connections for current user")
    public ResponseEntity<ApiResponse<Page<ConnectionDto>>> getMyConnections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<ConnectionDto> connections = connectionService.getMyConnections(currentUserId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(connections));
    }

    @GetMapping("/requests")
    @Operation(summary = "Get incoming connection requests")
    public ResponseEntity<ApiResponse<Page<ConnectionDto>>> getIncomingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<ConnectionDto> requests = connectionService.getIncomingRequests(currentUserId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "Accept an incoming connection request")
    public ResponseEntity<ApiResponse<ConnectionDto>> acceptConnection(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConnectionDto accepted = connectionService.acceptConnection(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Connection accepted", accepted));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject an incoming connection request")
    public ResponseEntity<ApiResponse<ConnectionDto>> rejectConnection(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConnectionDto rejected = connectionService.rejectConnection(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Connection rejected", rejected));
    }
}
