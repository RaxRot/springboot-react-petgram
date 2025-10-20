package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private CommentService commentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentResponse commentResponse;
    private CommentRequest commentRequest;
    private CommentPageResponse commentPageResponse;

    @BeforeEach
    void setUp() {
        UserResponseForSearch author = new UserResponseForSearch("Alice", "pic.png");
        commentResponse = new CommentResponse(1L, "Nice post!", LocalDateTime.now(), LocalDateTime.now(), author);
        commentRequest = new CommentRequest("Nice post!");
        commentPageResponse = new CommentPageResponse(List.of(commentResponse), 0, 10, 1, 1L, true);
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/comments — should create comment")
    void addComment_ShouldReturnCreated() throws Exception {
        Mockito.when(commentService.addComment(anyLong(), any(CommentRequest.class)))
                .thenReturn(commentResponse);

        mockMvc.perform(post("/api/posts/{postId}/comments", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Nice post!"));

        verify(commentService, times(1)).addComment(eq(5L), any(CommentRequest.class));
    }

    @Test
    @DisplayName("GET /api/public/posts/{postId}/comments — should return page of comments")
    void getComments_ShouldReturnPage() throws Exception {
        Mockito.when(commentService.getComments(anyLong(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(commentPageResponse);

        mockMvc.perform(get("/api/public/posts/{postId}/comments", 5L)
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "createdAt")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("Nice post!"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(commentService, times(1))
                .getComments(eq(5L), eq(0), eq(10), eq("createdAt"), eq("asc"));
    }

    @Test
    @DisplayName("PATCH /api/comments/{commentId} — should update comment")
    void updateComment_ShouldReturnUpdated() throws Exception {
        Mockito.when(commentService.updateComment(anyLong(), any(CommentRequest.class)))
                .thenReturn(commentResponse);

        mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Nice post!"));

        verify(commentService, times(1)).updateComment(eq(1L), any(CommentRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/comments/{commentId} — should delete comment")
    void deleteComment_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                .andExpect(status().isNoContent());

        verify(commentService, times(1)).deleteComment(1L);
    }

    @Test
    @DisplayName("GET /api/admin/comments — should return all comments")
    void getAllComments_ShouldReturnPage() throws Exception {
        Mockito.when(commentService.getAllComments(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(commentPageResponse);

        mockMvc.perform(get("/api/admin/comments")
                        .param("pageNumber", "0")
                        .param("pageSize", "20")
                        .param("sortBy", "createdAt")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("Nice post!"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(commentService, times(1))
                .getAllComments(eq(0), eq(20), eq("createdAt"), eq("desc"));
    }
}
