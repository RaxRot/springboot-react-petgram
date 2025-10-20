package com.raxrot.back.controllers;

import com.raxrot.back.services.FollowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private FollowService followService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/users/{userId}/follow — should follow user")
    void follow_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/follow", 5L))
                .andExpect(status().isNoContent());
        verify(followService, times(1)).followUser(5L);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/follow — should unfollow user")
    void unfollow_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/follow", 5L))
                .andExpect(status().isNoContent());
        verify(followService, times(1)).unfollowUser(5L);
    }

    @Test
    @DisplayName("GET /api/public/users/{userId}/followers/count — should return follower count")
    void getFollowersCount_ShouldReturnCount() throws Exception {
        Mockito.when(followService.getFollowersCount(anyLong())).thenReturn(42L);
        mockMvc.perform(get("/api/public/users/{userId}/followers/count", 7L))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
        verify(followService, times(1)).getFollowersCount(7L);
    }

    @Test
    @DisplayName("GET /api/public/users/{userId}/following/count — should return following count")
    void getFollowingCount_ShouldReturnCount() throws Exception {
        Mockito.when(followService.getFollowingCount(anyLong())).thenReturn(17L);
        mockMvc.perform(get("/api/public/users/{userId}/following/count", 9L))
                .andExpect(status().isOk())
                .andExpect(content().string("17"));
        verify(followService, times(1)).getFollowingCount(9L);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/follow/state — should return follow state")
    void followState_ShouldReturnFollowingStatus() throws Exception {
        Mockito.when(followService.isFollowing(anyLong())).thenReturn(true);
        mockMvc.perform(get("/api/users/{userId}/follow/state", 11L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
        verify(followService, times(1)).isFollowing(11L);
    }
}
