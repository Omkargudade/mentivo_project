package com.mentivo.dto.call;

public class CallSignalDto {

    private String type; // CALL_OFFER, CALL_ANSWER, ICE_CANDIDATE, CALL_REJECT, CALL_END, CALL_BUSY
    private String callId;
    private Long senderId;
    private String senderName;
    private String senderImage;
    private Long targetUserId;
    private Object sdp;
    private Object candidate;
    private String reason;
    private Long bookingId;
    private Long sessionId;
    private String sessionTitle;

    public CallSignalDto() {}

    public CallSignalDto(String type, String callId, Long senderId, String senderName, String senderImage, Long targetUserId) {
        this.type = type;
        this.callId = callId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderImage = senderImage;
        this.targetUserId = targetUserId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderImage() { return senderImage; }
    public void setSenderImage(String senderImage) { this.senderImage = senderImage; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public Object getSdp() { return sdp; }
    public void setSdp(Object sdp) { this.sdp = sdp; }

    public Object getCandidate() { return candidate; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
}
