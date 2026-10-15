package com.mentivo.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "mentor_availabilities", indexes = {
        @Index(name = "idx_avail_mentor_day", columnList = "mentor_id, day_of_week")
})
public class MentorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean isActive = true;

    public MentorAvailability() {}

    public MentorAvailability(MentorProfile mentor, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.mentor = mentor;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MentorProfile getMentor() { return mentor; }
    public void setMentor(MentorProfile mentor) { this.mentor = mentor; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
