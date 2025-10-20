package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.ChangePasswordRequest;
import com.raxrot.back.dtos.UpdateUsernameRequest;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.security.services.UserDetailsServiceImpl;
import com.raxrot.back.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse userResponse;
    private UserResponseForSearch userResponseForSearch;

    @BeforeEach
    void setup() {
        userResponse = new UserResponse(
                1L, "raxrot", "raxrot@example.com", "https://cdn.petgram.com/u1.jpg", false
        );
        userResponseForSearch = new UserResponseForSearch(
                "raxrot", "https://cdn.petgram.com/u1.jpg"
        );
    }

    @Test
    @DisplayName("PATCH /api/user/uploadimg — should upload profile picture successfully")
    void uploadImg_ShouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes()
        );

        when(userService.uploadImgProfilePic(any())).thenReturn(userResponse);

        mockMvc.perform(multipart("/api/user/uploadimg")
                        .file(file)
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userName").value("raxrot"))
                .andExpect(jsonPath("$.email").value("raxrot@example.com"))
                .andExpect(jsonPath("$.profilePic").value("https://cdn.petgram.com/u1.jpg"));

        verify(userService, times(1)).uploadImgProfilePic(any());
    }

    @Test
    @DisplayName("GET /api/user/username/{username} — should return user info")
    void getUserByUsername_ShouldReturnOk() throws Exception {
        when(userService.getUserByUsername("raxrot")).thenReturn(userResponseForSearch);

        mockMvc.perform(get("/api/user/username/{username}", "raxrot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("raxrot"))
                .andExpect(jsonPath("$.profilePic").value("https://cdn.petgram.com/u1.jpg"));

        verify(userService, times(1)).getUserByUsername("raxrot");
    }

    @Test
    @DisplayName("PATCH /api/user/username — should update username successfully")
    void updateUsername_ShouldReturnOk() throws Exception {
        UpdateUsernameRequest req = new UpdateUsernameRequest("newRaxrot");
        UserResponse updated = new UserResponse(
                1L, "newRaxrot", "raxrot@example.com", "https://cdn.petgram.com/u1.jpg", false
        );

        when(userService.updateUsername(any(UpdateUsernameRequest.class))).thenReturn(updated);
        when(jwtUtils.getJwtCookie(any())).thenReturn(ResponseCookie.from("jwt", "abc123").build());

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "raxrot", "raxrot@example.com", "password", Collections.emptyList()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/api/user/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("newRaxrot"))
                .andExpect(jsonPath("$.email").value("raxrot@example.com"));

        verify(userService, times(1)).updateUsername(any(UpdateUsernameRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/user/password — should update password successfully")
    void updatePassword_ShouldReturnNoContent() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("old123", "new12345", "new12345");

        when(jwtUtils.getCleanJwtCookie()).thenReturn(
                ResponseCookie.from("jwt", "").maxAge(0).build()
        );

        mockMvc.perform(patch("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updatePassword(any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/user/username — should return 400 for invalid username")
    void updateUsername_ShouldReturnBadRequest_WhenInvalid() throws Exception {
        UpdateUsernameRequest req = new UpdateUsernameRequest("");

        mockMvc.perform(patch("/api/user/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).updateUsername(any());
    }
}
