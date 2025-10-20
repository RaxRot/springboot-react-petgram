package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PublicUserResponse;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private com.raxrot.back.security.jwt.JwtUtils jwtUtils;
    @MockBean private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;
    @MockBean private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    private PublicUserResponse publicUserResponse;
    private User user;

    @BeforeEach
    void setup() {
        publicUserResponse = new PublicUserResponse();
        publicUserResponse.setId(1L);
        publicUserResponse.setUserName("raxrot");
        publicUserResponse.setProfilePic("https://cdn.petgram.com/u1.jpg");
        publicUserResponse.setFollowers(123);
        publicUserResponse.setFollowing(77);
        publicUserResponse.setBanned(false);

        user = new User();
        user.setUserId(1L);
        user.setUserName("raxrot");
    }

    @Test
    @DisplayName("GET /api/public/users/{username} — should return public profile")
    void getPublicUser_ShouldReturnOk() throws Exception {
        when(userService.getPublicUserByUsername("raxrot")).thenReturn(publicUserResponse);

        mockMvc.perform(get("/api/public/users/{username}", "raxrot")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("raxrot"))
                .andExpect(jsonPath("$.profilePic").value("https://cdn.petgram.com/u1.jpg"))
                .andExpect(jsonPath("$.followers").value(123))
                .andExpect(jsonPath("$.following").value(77))
                .andExpect(jsonPath("$.banned").value(false));

        verify(userService, times(1)).getPublicUserByUsername("raxrot");
    }

    @Test
    @DisplayName("GET /api/public/users/username/{username}/id — should return user ID")
    void getUserIdByUsername_ShouldReturnOk() throws Exception {
        when(userRepository.findByUserName("raxrot")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/public/users/username/{username}/id", "raxrot"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(userRepository, times(1)).findByUserName("raxrot");
    }

    @Test
    @DisplayName("GET /api/public/users/username/{username}/id — should return 404 if user not found")
    void getUserIdByUsername_ShouldReturnNotFound() throws Exception {
        when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/public/users/username/{username}/id", "unknown"))
                .andExpect(status().isNotFound());

        verify(userRepository, times(1)).findByUserName("unknown");
    }
}
