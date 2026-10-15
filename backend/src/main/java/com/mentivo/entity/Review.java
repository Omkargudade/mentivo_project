package com.mentivo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_mentor", columnList = "mentor_id"),
        @Index(name = "idx_review_learner", columnList = "learner_id"),
        @Index(name = "idx_review_rating", columnList = "rating")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private User learner;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Review() {}

    public Review(Booking booking, MentorProfile mentor, User learner, Integer rating, String reviewText) {
        this.booking = booking;
        this.mentor = mentor;
        this.learner = learner;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public MentorProfile getMentor() { return mentor; }
    public void setMentor(MentorProfile mentor) { this.mentor = mentor; }

    public User getLearner() { return learner; }
    public void setLearner(User learner) { this.learner = learner; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
