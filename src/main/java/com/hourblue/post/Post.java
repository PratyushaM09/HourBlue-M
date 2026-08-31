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
    @Column(length = 20)
    private Mood mood;

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
        this(category, slug, title, description, imageUrl, null, altText, null);
    }

    public Post(
            Category category,
            String slug,
            String title,
            String description,
            String imageUrl,
            String cloudinaryPublicId,
            String altText,
            String sourceUrl) {
        this(category, slug, title, description, imageUrl, cloudinaryPublicId, altText, sourceUrl, null);
    }

    public Post(
            Category category,
            String slug,
            String title,
            String description,
            String imageUrl,
            String cloudinaryPublicId,
            String altText,
            String sourceUrl,
            Mood mood) {
        this.category = Objects.requireNonNull(category);
        this.slug = Objects.requireNonNull(slug);
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.imageUrl = Objects.requireNonNull(imageUrl);
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.altText = Objects.requireNonNull(altText);
        this.sourceUrl = sourceUrl;
        this.mood = mood;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public String getAltText() {
        return altText;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Mood getMood() {
        return mood;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void publish(Instant publishedAt) {
        if (status != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be published.");
        }
        this.publishedAt = Objects.requireNonNull(publishedAt, "Publication timestamp is required.");
        status = PostStatus.PUBLISHED;
    }

    public void archive() {
        if (status == PostStatus.ARCHIVED) {
            return;
        }
        status = PostStatus.ARCHIVED;
    }

    public void updateMetadata(
            Category category,
            String title,
            String slug,
            String description,
            String altText,
            String sourceUrl,
            Mood mood) {
        if (category == null || title == null || slug == null || description == null || altText == null) {
            throw new IllegalArgumentException("Post metadata values are required.");
        }
        if (!this.slug.equals(slug) && status != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can change slug.");
        }
        this.category = category;
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.altText = altText;
        this.sourceUrl = sourceUrl;
        this.mood = mood;
    }

    public void replaceImage(String imageUrl, String cloudinaryPublicId) {
        if (imageUrl == null || imageUrl.isBlank()
                || cloudinaryPublicId == null || cloudinaryPublicId.isBlank()) {
            throw new IllegalArgumentException("Post image values are required.");
        }
        this.imageUrl = imageUrl;
        this.cloudinaryPublicId = cloudinaryPublicId;
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
