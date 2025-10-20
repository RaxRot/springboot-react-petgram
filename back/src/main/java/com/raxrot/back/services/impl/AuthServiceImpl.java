package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Role;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.RoleRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.services.AuthService;
import com.raxrot.back.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public ResponseEntity<UserInfoResponse> authenticate(LoginRequest loginRequest) {
        log.info("Attempting authentication for username: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            ResponseCookie jwtCookie = jwtUtils.getJwtCookie(userDetails);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            log.info("User '{}' authenticated successfully with roles: {}", userDetails.getUsername(), roles);

            UserInfoResponse resp = new UserInfoResponse(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(resp);

        } catch (AuthenticationException ex) {
            log.error("Authentication failed for username: {}", loginRequest.getUsername());
            throw new ApiException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public ResponseEntity<?> register(SignupRequest signUpRequest) {
        log.info("Attempting to register new user: {}", signUpRequest.getUsername());

        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", signUpRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username is already taken"));
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration failed: email '{}' already in use", signUpRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email is already in use"));
        }

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new IllegalStateException("Role ROLE_USER not found"));
            roles.add(userRole);
        } else {
            for (String r : strRoles) {
                AppRole appRole = switch (r.toLowerCase()) {
                    case "admin" -> AppRole.ROLE_ADMIN;
                    default -> AppRole.ROLE_USER;
                };
                Role role = roleRepository.findByRoleName(appRole)
                        .orElseThrow(() -> new IllegalStateException("Role " + appRole + " not found"));
                roles.add(role);
            }
        }

        user.setRoles(roles);
        User saved = userRepository.save(user);
        log.info("User '{}' registered successfully with roles: {}", saved.getUserName(), saved.getRoles());

        sendEmail(signUpRequest);

        Map<String, Object> resp = Map.of(
                "id", saved.getUserId(),
                "username", saved.getUserName(),
                "email", saved.getEmail(),
                "roles", saved.getRoles().stream().map(r -> r.getRoleName().name()).toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    private void sendEmail(SignupRequest signUpRequest) {
        log.info("Sending welcome email to: {}", signUpRequest.getEmail());
        emailService.sendEmail(
                signUpRequest.getEmail(),
                "🐾 Welcome to PetGram!",
                "Hello " + signUpRequest.getUsername() + "!\n\n" +
                        "🎉 Thanks for joining PetGram — the place where pets shine 🐶🐱\n\n" +
                        "👉 Your username for login is: " + signUpRequest.getUsername() + "\n\n" +
                        "Have fun sharing cute moments with your pets! 💕\n\n" +
                        "— The PetGram Team"
        );
        log.info("Welcome email sent successfully to: {}", signUpRequest.getEmail());
    }

    @Override
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        log.info("Returning user info for '{}'", userDetails.getUsername());

        UserInfoResponse resp = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<?> signout() {
        log.info("User signout initiated.");
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        log.info("JWT cookie cleared. Logout successful.");
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body("Logout successful");
    }
}
