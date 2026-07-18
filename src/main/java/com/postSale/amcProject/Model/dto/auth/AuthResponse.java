package com.postSale.amcProject.Model.dto.auth;

/** Wraps signup/login responses with a message and the authenticated user data. */
public record AuthResponse(
        String message,
        AuthUserResponse user
) {}

