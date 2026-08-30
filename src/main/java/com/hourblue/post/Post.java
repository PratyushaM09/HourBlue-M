package com.hourblue.post;

import java.time.Instant;
import java.util.Objects;

import com.hourblue.category.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "alt_text", nullable = false, length = 255)
    private String altText;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    protected Post() {
    }

    public Post(
            Category category,
            String slug,
            String title,
            String description,
            String imageUrl,
            String altText) {
        this.category = Objects.requireNonNull(category);
        this.slug = Objects.requireNonNull(slug);
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.imageUrl = Objects.requireNonNull(imageUrl);
        this.altText = Objects.requireNonNull(altText);
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
