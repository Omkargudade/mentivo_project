package com.mentivo.dto.chat;

import com.mentivo.dto.user.UserSummaryDto;
import java.time.LocalDateTime;

public class ConversationDto {
    private Long id;
    private UserSummaryDto peerUser;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private long unreadCount;

    public ConversationDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSummaryDto getPeerUser() { return peerUser; }
    public void setPeerUser(UserSummaryDto peerUser) { this.peerUser = peerUser; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}
