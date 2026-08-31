package com.hourblue.admin.category;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "Name is required.")
    @Size(max = 120, message = "Name must be 120 characters or fewer.")
    private String name;

    @NotBlank(message = "Slug is required.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase and use hyphen-separated words.")
    @Size(max = 120, message = "Slug must be 120 characters or fewer.")
    private String slug;

    @Size(max = 255, message = "Description must be 255 characters or fewer.")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void normalize() {
        name = trim(name);
        slug = trim(slug);
        if (slug != null) {
            slug = slug.toLowerCase(Locale.ROOT);
        }
        description = trim(description);
        if (description != null && description.isBlank()) {
            description = null;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
