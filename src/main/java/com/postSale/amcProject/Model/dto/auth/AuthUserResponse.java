package com.postSale.amcProject.Model.dto.auth;

/**
 * AuthUserResponse is the safe user object we return to the frontend.
 * It intentionally does NOT include the password hash.
 */
public record AuthUserResponse(
        String id,
        String name,
        String email
) {}

