package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.ForgotUsernameRequest;
import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import com.raxrot.back.services.AuthService;
import com.raxrot.back.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserInfoResponse userInfoResponse;
    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private ForgotUsernameRequest forgotUsernameRequest;

    @BeforeEach
    void setUp() {
        userInfoResponse = new UserInfoResponse(1L, "jwtToken123", "alice", "alice@example.com", List.of("ROLE_USER"));
        loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("password");
        signupRequest = new SignupRequest();
        signupRequest.setUsername("alice");
        signupRequest.setEmail("alice@example.com");
        signupRequest.setPassword("password");
        forgotUsernameRequest = new ForgotUsernameRequest();
        forgotUsernameRequest.setEmail("alice@example.com");
    }

    @Test
    @DisplayName("POST /api/auth/signin — should authenticate user")
    void authenticateUser_ShouldReturnUserInfoResponse() throws Exception {
        Mockito.when(authService.authenticate(any(LoginRequest.class)))
                .thenReturn(ResponseEntity.ok(userInfoResponse));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authService, times(1)).authenticate(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/signup — should register user")
    void registerUser_ShouldReturnOk() throws Exception {
        Mockito.when(authService.register(any(SignupRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok("User registered"));


        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        verify(authService, times(1)).register(any(SignupRequest.class));
    }

    @Test
    @DisplayName("GET /api/auth/user — should return user info when authenticated")
    void getUserInfo_ShouldReturnUserInfoResponse() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(() -> "ROLE_USER")
        );

        Mockito.when(authService.getUserInfo(any(Authentication.class)))
                .thenReturn(ResponseEntity.ok(userInfoResponse));

        mockMvc.perform(get("/api/auth/user")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authService, times(1)).getUserInfo(any(Authentication.class));
    }

    @Test
    @DisplayName("GET /api/auth/user — should return 401 when unauthenticated")
    void getUserInfo_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/username — should return username")
    void getCurrentUserName_ShouldReturnUsername() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", null);

        mockMvc.perform(get("/api/auth/username").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));
    }

    @Test
    @DisplayName("POST /api/auth/signout — should sign out user")
    void signoutUser_ShouldReturnOk() throws Exception {
        Mockito.when(authService.register(any(SignupRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Signed Out"));


        mockMvc.perform(post("/api/auth/signout"))
                .andExpect(status().isOk());

        verify(authService, times(1)).signout();
    }

    @Test
    @DisplayName("POST /api/auth/forgot-username — should send reminder")
    void remindUsername_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotUsernameRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("If this email exists, we sent you instructions"));

        verify(userService, times(1)).sendUsernameReminder(any(ForgotUsernameRequest.class));
    }
}
