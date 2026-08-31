package com.hourblue.admin.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class PostFormValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validFormPassesValidation() {
        PostForm form = form();

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);

        assertTrue(violations.isEmpty());
    }

    @Test
    void blankRequiredValuesAreRejected() {
        PostForm form = form();
        form.setTitle(" ");
        form.setSlug(" ");
        form.setDescription(" ");
        form.setAltText(" ");

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
    }

    @Test
    void oversizedValuesAreRejected() {
        PostForm form = form();
        form.setTitle("A".repeat(161));
        form.setSlug("A".repeat(161));
        form.setDescription("A".repeat(10001));
        form.setAltText("A".repeat(256));

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
    }

    @Test
    void lowercaseHyphenatedSlugIsAccepted() {
        PostForm form = form();
        form.setSlug("hello-world-123");

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);

        assertTrue(violations.isEmpty());
    }

    @Test
    void leadingTrailingAndRepeatedHyphenSlugsAreRejected() {
        assertFalse(validator.validate(form("-hello")).isEmpty());
        assertFalse(validator.validate(form("hello-")).isEmpty());
        assertFalse(validator.validate(form("hello--world")).isEmpty());
    }

    @Test
    void sourceUrlIsTrimmedAndBlankValueBecomesNull() {
        PostForm form = form();
        form.setSourceUrl("  https://example.com/article   ");

        form.normalize();

        assertEquals("https://example.com/article", form.getSourceUrl());

        PostForm blank = form();
        blank.setSourceUrl("   ");
        blank.normalize();

        assertTrue(blank.getSourceUrl() == null);
    }

    @Test
    void invalidSourceUrlsAreRejectedByServiceLayerValidation() {
        PostForm badScheme = form();
        badScheme.setSourceUrl("ftp://example.com/article");

        PostForm badHost = form();
        badHost.setSourceUrl("https://");

        assertFalse(validator.validate(badScheme).isEmpty());
        assertFalse(validator.validate(badHost).isEmpty());
    }

    private PostForm form() {
        PostForm form = new PostForm();
        form.setCategoryId(1L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("This is a worthwhile article.");
        form.setAltText("A friendly image");
        form.setSourceUrl("https://example.com/article");
        return form;
    }

    private PostForm form(String slug) {
        PostForm form = form();
        form.setSlug(slug);
        return form;
    }
}
