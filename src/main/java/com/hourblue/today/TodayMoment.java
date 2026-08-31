package com.hourblue.today;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.hourblue.post.Post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "today_moments",
        uniqueConstraints = @UniqueConstraint(name = "uk_today_moments_feature_date", columnNames = "feature_date"))
public class TodayMoment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_date", nullable = false)
    private LocalDate featureDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    protected TodayMoment() {
    }

    public TodayMoment(LocalDate featureDate, Post post) {
        this.featureDate = Objects.requireNonNull(featureDate);
        this.post = Objects.requireNonNull(post);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFeatureDate() {
        return featureDate;
    }

    public Post getPost() {
        return post;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void replacePost(Post post) {
        this.post = Objects.requireNonNull(post);
    }

    @PrePersist
    void setCreatedTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void setUpdatedTimestamp() {
        updatedAt = Instant.now();
    }
}
