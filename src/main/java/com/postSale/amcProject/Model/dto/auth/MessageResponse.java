package com.postSale.amcProject.Model.dto.auth;

/** Simple response carrying only a text message (used by logout, forgot password, reset password). */
public record MessageResponse(String message) {}

