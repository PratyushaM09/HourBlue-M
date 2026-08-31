package com.hourblue.post;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import com.hourblue.category.Category;

import org.junit.jupiter.api.Test;

class PostTests {

    @Test
    void cloudinaryDraftFieldsAreCaptured() {
        Category category = new Category("Design", "design", "Design");

        Post post = new Post(
                category,
                "cloudinary-draft",
                "Draft title",
                "Draft description",
                "https://cdn.example.com/image.jpg",
                "images/cloudinary-draft",
                "Descriptive alt text",
                "https://example.com/source");

        assertEquals(PostStatus.DRAFT, post.getStatus());
        assertEquals("https://cdn.example.com/image.jpg", post.getImageUrl());
        assertEquals("images/cloudinary-draft", post.getCloudinaryPublicId());
        assertEquals("https://example.com/source", post.getSourceUrl());
        assertEquals("Descriptive alt text", post.getAltText());
    }

    @Test
    void draftMetadataUpdateIsAllowed() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/image.jpg", "public-old", "Old alt", null);

        post.updateMetadata(category, "New title", "new-slug", "New description", "New alt", "https://example.com/new");

        assertEquals("new-slug", post.getSlug());
        assertEquals("New title", post.getTitle());
        assertEquals("New description", post.getDescription());
        assertEquals("New alt", post.getAltText());
        assertEquals("https://example.com/new", post.getSourceUrl());
        assertEquals("https://cdn.example.com/image.jpg", post.getImageUrl());
        assertEquals("public-old", post.getCloudinaryPublicId());
    }

    @Test
    void draftSlugCanChange() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/image.jpg", "public-old", "Old alt", null);

        assertDoesNotThrow(() -> post.updateMetadata(category, "Updated title", "new-slug", "Updated description", "Updated alt", null));
        assertEquals("new-slug", post.getSlug());
    }

    @Test
    void publishedSlugCannotChange() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/image.jpg", "public-old", "Old alt", null);
        post.publish(Instant.parse("2026-01-02T00:00:00Z"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> post.updateMetadata(category, "Updated title", "new-slug", "Updated description", "Updated alt", null));

        assertEquals("Only draft posts can change slug.", exception.getMessage());
    }

    @Test
    void unchangedPublishedSlugIsAllowed() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/image.jpg", "public-old", "Old alt", null);
        post.publish(Instant.parse("2026-01-02T00:00:00Z"));

        assertDoesNotThrow(() -> post.updateMetadata(category, "Updated title", "old-slug", "Updated description", "Updated alt", null));
        assertEquals("old-slug", post.getSlug());
    }

    @Test
    void draftPublishesOnce() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "draft-post", "Draft title", "Draft description", "https://cdn.example.com/image.jpg", "public-old", "Alt text", null);

        post.publish(Instant.parse("2026-01-02T00:00:00Z"));
        assertEquals(PostStatus.PUBLISHED, post.getStatus());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> post.publish(Instant.parse("2026-01-03T00:00:00Z")));

        assertEquals("Only draft posts can be published.", exception.getMessage());
    }

    @Test
    void archiveRetainsPublishedAtAndImageFields() {
        Category category = new Category("Design", "design", "Design");
        Instant publishedAt = Instant.parse("2026-01-02T00:00:00Z");
        Post post = new Post(category, "archived-post", "Draft title", "Draft description", "https://cdn.example.com/image.jpg", "public-old", "Alt text", "https://example.com/source");
        post.publish(publishedAt);

        post.archive();

        assertEquals(PostStatus.ARCHIVED, post.getStatus());
        assertEquals(publishedAt, post.getPublishedAt());
        assertEquals("https://cdn.example.com/image.jpg", post.getImageUrl());
        assertEquals("public-old", post.getCloudinaryPublicId());
    }
}
