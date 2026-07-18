package com.postSale.amcProject.Services;

import com.postSale.amcProject.Model.dto.auth.*;
import com.postSale.amcProject.Model.nodes.User;
import com.postSale.amcProject.Repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * AuthService contains all authentication business logic:
 * signup, login, forgot password, reset password, logout, and session restore.
 */
@Service
public class AuthService {

    // Reset tokens expire after 30 minutes
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Base URL used to build the password reset link (e.g. http://localhost:4200)
    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ─── SIGNUP ────────────────────────────────────────────────────────────────

    /**
     * Creates a new user account, hashes their password, and logs them in.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());

        // Check if email is already taken
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Build and save the user node
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password())); // BCrypt hash
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User saved = userRepository.save(user);

        // Automatically log the user in after signup
        createSession(saved, httpRequest);

        return new AuthResponse("Signup successful", toDto(saved));
    }

    // ─── LOGIN ─────────────────────────────────��───────────────────────────────

    /**
     * Validates email + password and creates an authenticated session.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify the password against the stored BCrypt hash
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Create server-side session
        createSession(user, httpRequest);

        return new AuthResponse("Login successful", toDto(user));
    }

    // ─── CURRENT USER ──────────────────────────────────────────────────────────

    /**
     * Returns the currently logged-in user's data.
     * Spring Security injects the Authentication from the session automatically.
     */
    @Transactional(readOnly = true)
    public Optional<AuthUserResponse> currentUser(Authentication authentication) {
        // Check if the user is actually authenticated (not an anonymous guest)
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        String email = normalizeEmail(authentication.getName());
        return userRepository.findByEmail(email).map(this::toDto);
    }

    // ─── FORGOT PASSWORD ───────────────────────────────────────────────────────

    /**
     * Generates a secure reset token, saves it on the user, and sends a reset email.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        // Generate a secure random token (URL-safe base64, 32 bytes = 43 chars)
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES);

        // Save the token and expiry on the user node
        user.setResetToken(token);
        user.setResetTokenExpiresAt(expiresAt);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Build the reset link and send it
        String resetUrl = appBaseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl, expiresAt);

        return new MessageResponse("Password reset link has been sent to your email");
    }

    // ─── RESET PASSWORD ────────────────────────────────────────────────────────

    /**
     * Validates the reset token, then updates the user's password.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        // Find user by token
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        // Check if token has expired
        if (user.getResetTokenExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new IllegalArgumentException("Expired reset token");
        }

        // Update password with BCrypt hash
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        // Clear the reset token so it can't be reused
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new MessageResponse("Password has been reset successfully. Please log in.");
    }

    // ─── LOGOUT ─────────────────────────────────────────────────────────���──────

    /**
     * Destroys the server-side session and clears the security context.
     */
    public MessageResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // false = don't create a new session
        if (session != null) {
            session.invalidate(); // Destroys the session and its data
        }
        SecurityContextHolder.clearContext();
        return new MessageResponse("Logout successful");
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Creates a Spring Security session for the given user.
     * This is what makes the browser "stay logged in" — the session ID is stored
     * in a cookie (JSESSIONID) and Spring reads it on every subsequent request.
     */
    private void createSession(User user, HttpServletRequest request) {
        // Create an authentication token (username = email, no credentials needed here)
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Put it in the security context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Persist the context in the HTTP session so Spring can restore it on the next request
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    /** Trims and lowercases an email address to avoid case-sensitivity issues. */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** Maps a User node to a safe DTO (no password included). */
    private AuthUserResponse toDto(User user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail());
    }

    /** Generates a cryptographically secure, URL-safe random token. */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

