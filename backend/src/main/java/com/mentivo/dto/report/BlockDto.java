package com.mentivo.dto.report;

import com.mentivo.dto.user.UserSummaryDto;
import java.time.LocalDateTime;

public class BlockDto {
    private Long id;
    private UserSummaryDto blockedUser;
    private LocalDateTime createdAt;

    public BlockDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSummaryDto getBlockedUser() { return blockedUser; }
    public void setBlockedUser(UserSummaryDto blockedUser) { this.blockedUser = blockedUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
