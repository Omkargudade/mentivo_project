package com.mentivo.dto.report;

import jakarta.validation.constraints.NotNull;

public class BlockRequest {

    @NotNull(message = "User ID to block is required")
    private Long blockedUserId;

    public BlockRequest() {}

    public Long getBlockedUserId() { return blockedUserId; }
    public void setBlockedUserId(Long blockedUserId) { this.blockedUserId = blockedUserId; }
}
