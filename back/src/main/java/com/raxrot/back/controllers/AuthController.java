package com.raxrot.back.controllers;

import com.raxrot.back.dtos.ForgotUsernameRequest;
import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import com.raxrot.back.services.AuthService;
import com.raxrot.back.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Authentication",
        description = "Endpoints for user registration, login, logout, and account recovery."
)
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
            summary = "Sign in",
            description = "Authenticate a user using username and password. Returns a JWT token on success.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User authenticated successfully",
                            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid username or password"),
                    @ApiResponse(responseCode = "400", description = "Bad request")
            }
    )
    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("🔐 Signin attempt for username: {}", loginRequest.getUsername());
        ResponseEntity<UserInfoResponse> response = authService.authenticate(loginRequest);
        log.info("✅ Signin response status: {}", response.getStatusCode());
        return response;
    }

    @Operation(
            summary = "Sign up",
            description = "Register a new user account with a unique username and email address.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid registration data or user already exists")
            }
    )
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("📝 Signup attempt for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        ResponseEntity<?> response = authService.register(signUpRequest);
        log.info("✅ Signup completed with status: {}", response.getStatusCode());
        return response;
    }

    @Operation(
            summary = "Get current user info",
            description = "Returns information about the currently authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully fetched user info",
                            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token missing or invalid")
            }
    )
    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("⚠️ Unauthorized access attempt to /user endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("👤 Fetching user info for: {}", authentication.getName());
        ResponseEntity<UserInfoResponse> response = authService.getUserInfo(authentication);
        log.info("✅ User info fetched successfully for {}", authentication.getName());
        return response;
    }

    @Operation(
            summary = "Get current username",
            description = "Returns only the username of the currently logged-in user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Username fetched successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token missing or invalid")
            }
    )
    @GetMapping("/username")
    public String getCurrentUserName(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "NULL";
        log.info("🔎 Current username request: {}", username);
        return username;
    }

    @Operation(
            summary = "Sign out",
            description = "Logs out the currently authenticated user (token invalidation or cookie removal).",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User signed out successfully")
            }
    )
    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        log.info("🚪 Signout request received");
        ResponseEntity<?> response = authService.signout();
        log.info("✅ User signed out successfully");
        return response;
    }

    @Operation(
            summary = "Forgot username",
            description = "Sends a reminder email to the user containing their username if the email exists.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reminder email sent successfully (if user exists)"),
                    @ApiResponse(responseCode = "400", description = "Invalid email format")
            }
    )
    @PostMapping("/forgot-username")
    public ResponseEntity<String> remindUsername(@Valid @RequestBody ForgotUsernameRequest req) {
        log.info("📧 Forgot username request for email: {}", req.getEmail());
        userService.sendUsernameReminder(req);
        log.info("✅ Username reminder email sent (if account exists)");
        return ResponseEntity.ok("If this email exists, we sent you instructions");
    }
}
