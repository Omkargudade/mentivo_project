package com.mentivo.dto.admin;

public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long totalMentors;
    private long totalLearners;
    private long onlineMentors;
    private long totalBookings;
    private long completedSessions;
    private Double totalRevenue;
    private Double totalPlatformFees;
    private long pendingReports;
    private long pendingVerifications;

    public AdminStatsDto() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getTotalMentors() { return totalMentors; }
    public void setTotalMentors(long totalMentors) { this.totalMentors = totalMentors; }

    public long getTotalLearners() { return totalLearners; }
    public void setTotalLearners(long totalLearners) { this.totalLearners = totalLearners; }

    public long getOnlineMentors() { return onlineMentors; }
    public void setOnlineMentors(long onlineMentors) { this.onlineMentors = onlineMentors; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getTotalPlatformFees() { return totalPlatformFees; }
    public void setTotalPlatformFees(Double totalPlatformFees) { this.totalPlatformFees = totalPlatformFees; }

    public long getPendingReports() { return pendingReports; }
    public void setPendingReports(long pendingReports) { this.pendingReports = pendingReports; }

    public long getPendingVerifications() { return pendingVerifications; }
    public void setPendingVerifications(long pendingVerifications) { this.pendingVerifications = pendingVerifications; }
}
