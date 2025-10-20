package com.raxrot.back.controllers;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.services.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private StoryService storyService;
    @MockBean private com.raxrot.back.security.jwt.JwtUtils jwtUtils;
    @MockBean private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;
    @MockBean private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    private StoryResponse storyResponse;

    @BeforeEach
    void setup() {
        storyResponse = new StoryResponse();
        storyResponse.setId(1L);
        storyResponse.setImageUrl("https://cdn.petgram.com/story1.jpg");
        storyResponse.setCreatedAt(LocalDateTime.now());
        storyResponse.setAuthorId(5L);
        storyResponse.setAuthorUsername("raxrot");
    }

    @Test
    @DisplayName("POST /api/stories — should create story")
    void createStory_ShouldReturnCreated() throws Exception {
        MockMultipartFile mockFile =
                new MockMultipartFile("file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "fakeimg".getBytes());

        when(storyService.create(any())).thenReturn(storyResponse);

        mockMvc.perform(multipart("/api/stories").file(mockFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.petgram.com/story1.jpg"))
                .andExpect(jsonPath("$.authorUsername").value("raxrot"))
                .andExpect(jsonPath("$.authorId").value(5));

        verify(storyService, times(1)).create(any());
    }

    @Test
    @DisplayName("GET /api/stories/my — should return current user's stories")
    void getMyStories_ShouldReturnOk() throws Exception {
        Page<StoryResponse> storyPage = new PageImpl<>(List.of(storyResponse));
        when(storyService.myStories(anyInt(), anyInt())).thenReturn(storyPage);

        mockMvc.perform(get("/api/stories/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorUsername").value("raxrot"))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://cdn.petgram.com/story1.jpg"));

        verify(storyService, times(1)).myStories(0, 10);
    }

    @Test
    @DisplayName("GET /api/stories/following — should return following stories")
    void getFollowingStories_ShouldReturnOk() throws Exception {
        Page<StoryResponse> storyPage = new PageImpl<>(List.of(storyResponse));
        when(storyService.followingStories(anyInt(), anyInt())).thenReturn(storyPage);

        mockMvc.perform(get("/api/stories/following")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorUsername").value("raxrot"));

        verify(storyService, times(1)).followingStories(0, 10);
    }

    @Test
    @DisplayName("DELETE /api/stories/{id} — should delete story")
    void deleteStory_ShouldReturnNoContent() throws Exception {
        Mockito.doNothing().when(storyService).delete(1L);

        mockMvc.perform(delete("/api/stories/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(storyService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("GET /api/public/stories — should return public stories")
    void getPublicStories_ShouldReturnOk() throws Exception {
        Page<StoryResponse> storyPage = new PageImpl<>(List.of(storyResponse));
        when(storyService.getAllStories(anyInt(), anyInt())).thenReturn(storyPage);

        mockMvc.perform(get("/api/public/stories")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://cdn.petgram.com/story1.jpg"));

        verify(storyService, times(1)).getAllStories(0, 10);
    }

    @Test
    @DisplayName("GET /api/public/stories/{id} — should view story")
    void viewStory_ShouldReturnOk() throws Exception {
        when(storyService.viewStory(1L)).thenReturn(storyResponse);

        mockMvc.perform(get("/api/public/stories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.authorUsername").value("raxrot"));

        verify(storyService, times(1)).viewStory(1L);
    }
}
