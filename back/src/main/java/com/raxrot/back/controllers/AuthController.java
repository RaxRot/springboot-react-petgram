package com.raxrot.back.controllers;

import com.raxrot.back.dtos.ForgotUsernameRequest;
import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import com.raxrot.back.services.AuthService;
import com.raxrot.back.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticate(loginRequest);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return authService.register(signUpRequest);
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return authService.getUserInfo(authentication);
    }

    @GetMapping("/username")
    public String getCurrentUserName(Authentication authentication) {
        return (authentication != null) ? authentication.getName() : "NULL";
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        return authService.signout();
    }

    @PostMapping("/forgot-username")
    public ResponseEntity<String>remindUsername(@Valid @RequestBody ForgotUsernameRequest req) {
        userService.sendUsernameReminder(req);
        return ResponseEntity.ok("If this email exists, we sent you instructions");
    }
}
