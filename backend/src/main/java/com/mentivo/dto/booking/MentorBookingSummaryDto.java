package com.mentivo.dto.booking;

public class MentorBookingSummaryDto {

    private long totalBookings;
    private long uniqueLearners;
    private long upcomingSessions;
    private long completedSessions;
    private Double totalEarnings;

    public MentorBookingSummaryDto() {}

    public MentorBookingSummaryDto(long totalBookings, long uniqueLearners, long upcomingSessions, long completedSessions, Double totalEarnings) {
        this.totalBookings = totalBookings;
        this.uniqueLearners = uniqueLearners;
        this.upcomingSessions = upcomingSessions;
        this.completedSessions = completedSessions;
        this.totalEarnings = totalEarnings != null ? totalEarnings : 0.0;
    }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getUniqueLearners() { return uniqueLearners; }
    public void setUniqueLearners(long uniqueLearners) { this.uniqueLearners = uniqueLearners; }

    public long getUpcomingSessions() { return upcomingSessions; }
    public void setUpcomingSessions(long upcomingSessions) { this.upcomingSessions = upcomingSessions; }

    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }

    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }
}
