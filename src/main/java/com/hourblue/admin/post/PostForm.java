package com.hourblue.admin.post;

import java.net.URI;
import java.util.Locale;

import com.hourblue.post.Mood;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PostForm {

    @NotNull(message = "Category is required.")
    private Long categoryId;

    @NotBlank(message = "Title is required.")
    @Size(max = 160, message = "Title must be 160 characters or fewer.")
    private String title;

    @NotBlank(message = "Slug is required.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase and use hyphen-separated words.")
    @Size(max = 160, message = "Slug must be 160 characters or fewer.")
    private String slug;

    @NotBlank(message = "Description is required.")
    @Size(max = 10000, message = "Description must be 10,000 characters or fewer.")
    private String description;

    @NotBlank(message = "Alt text is required.")
    @Size(max = 255, message = "Alt text must be 255 characters or fewer.")
    private String altText;

    @Size(max = 2048, message = "Source URL must be 2,048 characters or fewer.")
    private String sourceUrl;

    private Mood mood;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public void normalize() {
        title = normalizeText(title);
        slug = normalizeSlug(slug);
        altText = normalizeText(altText);
        sourceUrl = normalizeOptionalUrl(sourceUrl);
    }

    @AssertTrue(message = "Source URL must be a valid http or https URL.")
    public boolean isSourceUrlValid() {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return true;
        }

        try {
            URI uri = URI.create(sourceUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.isAbsolute()
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSlug(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
