package com.mentivo.dto.booking;

import java.math.BigDecimal;

public class BookingPriceCalculationDto {
    private BigDecimal hourlyRate;
    private Integer durationMinutes;
    private BigDecimal sessionPrice;
    private BigDecimal platformFee;
    private Double commissionPercent;
    private BigDecimal totalAmount;
    private BigDecimal mentorEarnings;

    public BookingPriceCalculationDto() {}

    public BookingPriceCalculationDto(BigDecimal hourlyRate, Integer durationMinutes, BigDecimal sessionPrice,
                                     BigDecimal platformFee, Double commissionPercent, BigDecimal totalAmount,
                                     BigDecimal mentorEarnings) {
        this.hourlyRate = hourlyRate;
        this.durationMinutes = durationMinutes;
        this.sessionPrice = sessionPrice;
        this.platformFee = platformFee;
        this.commissionPercent = commissionPercent;
        this.totalAmount = totalAmount;
        this.mentorEarnings = mentorEarnings;
    }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getSessionPrice() { return sessionPrice; }
    public void setSessionPrice(BigDecimal sessionPrice) { this.sessionPrice = sessionPrice; }

    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }

    public Double getCommissionPercent() { return commissionPercent; }
    public void setCommissionPercent(Double commissionPercent) { this.commissionPercent = commissionPercent; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getMentorEarnings() { return mentorEarnings; }
    public void setMentorEarnings(BigDecimal mentorEarnings) { this.mentorEarnings = mentorEarnings; }
}
