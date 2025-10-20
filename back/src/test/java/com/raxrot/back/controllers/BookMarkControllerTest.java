package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.services.BookMarkService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookMarkController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookMarkControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private BookMarkService bookmarkService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PostPageResponse postPageResponse;

    @BeforeEach
    void setUp() {
        UserResponseForSearch user = new UserResponseForSearch("alice", "pic.jpg");
        PostResponse post = new PostResponse(
                1L, "Cute Dog", "Lovely puppy photo", "img.jpg",
                AnimalType.DOG, LocalDateTime.now(), LocalDateTime.now(), user, 123
        );
        postPageResponse = new PostPageResponse(List.of(post), 0, 10, 1, 1L, true);
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/bookmarks — should add bookmark")
    void addBookmark_ShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/bookmarks", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value("Bookmark added"));

        verify(bookmarkService, times(1)).addBookmark(1L);
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/bookmarks — should remove bookmark")
    void deleteBookmark_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/bookmarks", 1L))
                .andExpect(status().isNoContent());

        verify(bookmarkService, times(1)).removeBookmark(1L);
    }

    @Test
    @DisplayName("GET /api/user/bookmarks — should return bookmarks list")
    void getBookmarks_ShouldReturnPostPageResponse() throws Exception {
        Mockito.when(bookmarkService.getMyBookmarks(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(postPageResponse);

        mockMvc.perform(get("/api/user/bookmarks")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "createdAt")
                        .param("sortOrder", "asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cute Dog"))
                .andExpect(jsonPath("$.content[0].animalType").value("DOG"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookmarkService, times(1)).getMyBookmarks(0, 10, "createdAt", "asc");
    }
}
