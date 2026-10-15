package com.mentivo.dto.call;

public class CallSessionCompleteRequest {
    private String callId;
    private Long peerUserId;
    private Integer durationSeconds;
    private Long bookingId;

    public CallSessionCompleteRequest() {}

    public CallSessionCompleteRequest(String callId, Long peerUserId, Integer durationSeconds, Long bookingId) {
        this.callId = callId;
        this.peerUserId = peerUserId;
        this.durationSeconds = durationSeconds;
        this.bookingId = bookingId;
    }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public Long getPeerUserId() { return peerUserId; }
    public void setPeerUserId(Long peerUserId) { this.peerUserId = peerUserId; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
}
