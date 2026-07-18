package com.postSale.amcProject.Model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SignupRequest is the data we expect in the request body when a user signs up.
 * Java record = immutable data holder with auto-generated constructor/getters.
 * @NotBlank and @Email trigger validation errors if the frontend sends bad data.
 */
public record SignupRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String password
) {}

