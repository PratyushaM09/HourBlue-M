package com.hourblue.subscriber;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubscriptionForm {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 320, message = "Email must be 320 characters or fewer.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void trimEmail() {
        if (email != null) {
            email = email.trim();
        }
    }
}
