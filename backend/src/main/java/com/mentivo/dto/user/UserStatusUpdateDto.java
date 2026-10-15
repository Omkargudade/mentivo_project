package com.mentivo.dto.user;

import com.mentivo.entity.enums.OnlineStatus;
import java.time.LocalDateTime;

public class UserStatusUpdateDto {
    private Long userId;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastSeen;
    private Long mentorProfileId;

    public UserStatusUpdateDto() {}

    public UserStatusUpdateDto(Long userId, OnlineStatus onlineStatus, LocalDateTime lastSeen, Long mentorProfileId) {
        this.userId = userId;
        this.onlineStatus = onlineStatus;
        this.lastSeen = lastSeen;
        this.mentorProfileId = mentorProfileId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Long getMentorProfileId() { return mentorProfileId; }
    public void setMentorProfileId(Long mentorProfileId) { this.mentorProfileId = mentorProfileId; }
}
