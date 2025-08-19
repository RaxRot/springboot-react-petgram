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
import com.raxrot.back.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);

        authService = new AuthServiceImpl(jwtUtils, authenticationManager, userRepository, roleRepository, passwordEncoder, emailService);
    }

    // --- authenticate ---

    @Test
    void authenticate_success() {
        LoginRequest req = new LoginRequest("john", "pwd");

        UserDetailsImpl userDetails = new UserDetailsImpl(1L, "john", "john@mail.com", "pwd",
                List.of(() -> "ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.getJwtCookie(userDetails)).thenReturn(ResponseCookie.from("jwt", "token").build());

        ResponseEntity<UserInfoResponse> resp = authService.authenticate(req);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("john", Objects.requireNonNull(resp.getBody()).getUsername());
        assertTrue(resp.getHeaders().containsKey("Set-Cookie"));
    }

    @Test
    void authenticate_invalidCredentials() {
        LoginRequest req = new LoginRequest("john", "bad");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad creds") {});

        assertThrows(ApiException.class, () -> authService.authenticate(req));
    }

    // --- register ---

    @Test
    void register_successWithDefaultRole() {
        SignupRequest signup = new SignupRequest();
        signup.setUsername("newuser");
        signup.setEmail("new@mail.com");
        signup.setPassword("pwd");

        when(userRepository.existsByUserName("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);

        Role userRole = new Role(AppRole.ROLE_USER);
        when(roleRepository.findByRoleName(AppRole.ROLE_USER)).thenReturn(Optional.of(userRole));

        User saved = new User("newuser", "new@mail.com", "encoded");
        saved.setUserId(10L);
        saved.setRoles(Set.of(userRole));

        when(passwordEncoder.encode("pwd")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ResponseEntity<?> resp = authService.register(signup);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals("newuser", body.get("username"));
        assertEquals(List.of("ROLE_USER"), body.get("roles"));

        verify(emailService, times(1))
                .sendEmail(eq("new@mail.com"), anyString(), contains("newuser"));
    }

    @Test
    void register_usernameConflict() {
        SignupRequest signup = new SignupRequest();
        signup.setUsername("exists");
        signup.setEmail("mail@mail.com");

        when(userRepository.existsByUserName("exists")).thenReturn(true);

        ResponseEntity<?> resp = authService.register(signup);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertTrue(((Map<?, ?>) resp.getBody()).get("message").toString().contains("Username"));
    }

    @Test
    void register_emailConflict() {
        SignupRequest signup = new SignupRequest();
        signup.setUsername("user");
        signup.setEmail("exists@mail.com");

        when(userRepository.existsByUserName("user")).thenReturn(false);
        when(userRepository.existsByEmail("exists@mail.com")).thenReturn(true);

        ResponseEntity<?> resp = authService.register(signup);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertTrue(((Map<?, ?>) resp.getBody()).get("message").toString().contains("Email"));
    }

    @Test
    void register_withAdminRole() {
        SignupRequest signup = new SignupRequest();
        signup.setUsername("boss");
        signup.setEmail("boss@mail.com");
        signup.setPassword("pwd");
        signup.setRole(Set.of("admin"));

        when(userRepository.existsByUserName("boss")).thenReturn(false);
        when(userRepository.existsByEmail("boss@mail.com")).thenReturn(false);

        Role adminRole = new Role(AppRole.ROLE_ADMIN);
        when(roleRepository.findByRoleName(AppRole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        User saved = new User("boss", "boss@mail.com", "enc");
        saved.setUserId(99L);
        saved.setRoles(Set.of(adminRole));

        when(passwordEncoder.encode("pwd")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ResponseEntity<?> resp = authService.register(signup);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals(List.of("ROLE_ADMIN"), body.get("roles"));
    }

    // --- getUserInfo ---

    @Test
    void getUserInfo_returnsInfo() {
        UserDetailsImpl userDetails = new UserDetailsImpl(5L, "john", "john@mail.com", "pwd",
                List.of(() -> "ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        ResponseEntity<UserInfoResponse> resp = authService.getUserInfo(auth);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("john", resp.getBody().getUsername());
        assertEquals(List.of("ROLE_USER"), resp.getBody().getRoles());
    }

    // --- signout ---

    @Test
    void signout_returnsCookie() {
        when(jwtUtils.getCleanJwtCookie()).thenReturn(ResponseCookie.from("jwt", "").build());

        ResponseEntity<?> resp = authService.signout();

        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getHeaders().containsKey("Set-Cookie"));
        assertEquals("Logout successful", resp.getBody());
    }
}
