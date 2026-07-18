package com.postSale.amcProject.Model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Data expected when a user requests a password reset email. */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email
) {}

