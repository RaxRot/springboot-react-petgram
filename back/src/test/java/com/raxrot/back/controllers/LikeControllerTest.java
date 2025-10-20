package com.raxrot.back.controllers;

import com.raxrot.back.services.LikeService;
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

@WebMvcTest(controllers = LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class LikeControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LikeService likeService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/posts/{postId}/likes — should like post and return updated count")
    void likePost_ShouldReturnLikesCount() throws Exception {
        Mockito.when(likeService.getLikesCount(anyLong())).thenReturn(42L);

        mockMvc.perform(post("/api/posts/{postId}/likes", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(42));

        verify(likeService, times(1)).likePost(10L);
        verify(likeService, times(1)).getLikesCount(10L);
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/likes — should unlike post and return updated count")
    void dislikePost_ShouldReturnLikesCount() throws Exception {
        Mockito.when(likeService.getLikesCount(anyLong())).thenReturn(41L);

        mockMvc.perform(delete("/api/posts/{postId}/likes", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(41));

        verify(likeService, times(1)).unlikePost(10L);
        verify(likeService, times(1)).getLikesCount(10L);
    }

    @Test
    @DisplayName("GET /api/public/posts/{postId}/likes — should return total likes")
    void getLikes_ShouldReturnLikesCount() throws Exception {
        Mockito.when(likeService.getLikesCount(anyLong())).thenReturn(12L);

        mockMvc.perform(get("/api/public/posts/{postId}/likes", 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(12));

        verify(likeService, times(1)).getLikesCount(5L);
    }
}
