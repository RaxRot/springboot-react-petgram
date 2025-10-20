package com.raxrot.back.controllers;

import com.raxrot.back.dtos.ForgotUsernameRequest;
import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import com.raxrot.back.services.AuthService;
import com.raxrot.back.services.UserService;
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
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("🔐 Signin attempt for username: {}", loginRequest.getUsername());
        ResponseEntity<UserInfoResponse> response = authService.authenticate(loginRequest);
        log.info("✅ Signin response status: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("📝 Signup attempt for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        ResponseEntity<?> response = authService.register(signUpRequest);
        log.info("✅ Signup completed with status: {}", response.getStatusCode());
        return response;
    }

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

    @GetMapping("/username")
    public String getCurrentUserName(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "NULL";
        log.info("🔎 Current username request: {}", username);
        return username;
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        log.info("🚪 Signout request received");
        ResponseEntity<?> response = authService.signout();
        log.info("✅ User signed out successfully");
        return response;
    }

    @PostMapping("/forgot-username")
    public ResponseEntity<String> remindUsername(@Valid @RequestBody ForgotUsernameRequest req) {
        log.info("📧 Forgot username request for email: {}", req.getEmail());
        userService.sendUsernameReminder(req);
        log.info("✅ Username reminder email sent (if account exists)");
        return ResponseEntity.ok("If this email exists, we sent you instructions");
    }
}
