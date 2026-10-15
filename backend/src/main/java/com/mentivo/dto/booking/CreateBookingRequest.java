package com.mentivo.dto.booking;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateBookingRequest {

    private Long mentorId;

    private Long topicId;

    private Long sessionId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private Integer durationMinutes;

    private String meetingNotes;

    public CreateBookingRequest() {}

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getMeetingNotes() { return meetingNotes; }
    public void setMeetingNotes(String meetingNotes) { this.meetingNotes = meetingNotes; }
}
