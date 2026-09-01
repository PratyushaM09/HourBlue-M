package com.hourblue.subscriber;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SubscriptionFormValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validEmailPassesValidation() {
        assertTrue(validator.validate(form("user@example.test")).isEmpty());
    }

    @Test
    void blankEmailIsRejected() {
        assertFalse(validator.validate(form(" ")).isEmpty());
    }

    @Test
    void malformedEmailIsRejected() {
        assertFalse(validator.validate(form("not-an-email")).isEmpty());
    }

    @Test
    void oversizedEmailIsRejected() {
        Set<ConstraintViolation<SubscriptionForm>> violations =
                validator.validate(form("a".repeat(309) + "@example.test"));

        assertFalse(violations.isEmpty());
    }

    private SubscriptionForm form(String email) {
        SubscriptionForm form = new SubscriptionForm();
        form.setEmail(email);
        return form;
    }
}
