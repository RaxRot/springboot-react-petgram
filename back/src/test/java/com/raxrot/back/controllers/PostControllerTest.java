package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Post;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private com.raxrot.back.security.jwt.JwtUtils jwtUtils;
    @MockBean private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;
    @MockBean private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean private PostService postService;
    @MockBean private PostRepository postRepository;
    @MockBean private ModelMapper modelMapper;

    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private PostRequest postRequest;
    private PostResponse postResponse;
    private PostPageResponse postPageResponse;

    @BeforeEach
    void setup() {
        postRequest = new PostRequest();
        postRequest.setTitle("Cute Cat");
        postRequest.setContent("My cat is adorable!");
        postRequest.setAnimalType(AnimalType.CAT);

        postResponse = new PostResponse();
        postResponse.setId(1L);
        postResponse.setTitle("Cute Cat");
        postResponse.setContent("My cat is adorable!");
        postResponse.setAnimalType(AnimalType.CAT);

        postPageResponse = new PostPageResponse(List.of(postResponse), 0, 10, 1, 1L, true);
    }

    @Test
    @DisplayName("POST /api/posts — should create post")
    void createPost_ShouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cat.jpg", "image/jpeg", "img".getBytes());
        MockMultipartFile json = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(postRequest));

        when(postService.createPost(any(), any())).thenReturn(postResponse);

        mockMvc.perform(multipart("/api/posts")
                        .file(file)
                        .file(json)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Cute Cat"))
                .andExpect(jsonPath("$.animalType").value("CAT"));

        verify(postService, times(1)).createPost(any(), any());
    }

    @Test
    @DisplayName("GET /api/public/posts — should return all posts")
    void getAllPosts_ShouldReturnOk() throws Exception {
        when(postService.getAllPosts(anyInt(), anyInt(), anyString(), anyString())).thenReturn(postPageResponse);

        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cute Cat"));
    }

    @Test
    @DisplayName("GET /api/public/users/{username}/posts — should return user posts")
    void getAllPostsByUsername_ShouldReturnOk() throws Exception {
        when(postService.getPostsByUsername(anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(postPageResponse);

        mockMvc.perform(get("/api/public/users/{username}/posts", "raxrot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].animalType").value("CAT"));
    }

    @Test
    @DisplayName("GET /api/public/posts/animal/{type} — should return posts by animal type")
    void getAllPostsByAnimalType_ShouldReturnOk() throws Exception {
        when(postService.getPostsByAnimalType(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(postPageResponse);

        mockMvc.perform(get("/api/public/posts/animal/{type}", "CAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cute Cat"));
    }

    @Test
    @DisplayName("GET /api/public/posts/{id} — should return post by id")
    void getPostById_ShouldReturnOk() throws Exception {
        when(postService.getPostById(anyLong())).thenReturn(postResponse);

        mockMvc.perform(get("/api/public/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cute Cat"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} — should delete post")
    void deletePost_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(postService, times(1)).deletePost(1L);
    }

    @Test
    @DisplayName("GET /api/posts/feed/following — should return following feed")
    void getFollowingFeed_ShouldReturnOk() throws Exception {
        when(postService.getFollowingFeed(anyInt(), anyInt(), anyString(), anyString())).thenReturn(postPageResponse);

        mockMvc.perform(get("/api/posts/feed/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cute Cat"));
    }

    @Test
    @DisplayName("GET /api/public/posts/trending — should return trending posts")
    void getTrending_ShouldReturnOk() throws Exception {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Cute Cat");
        post.setAnimalType(AnimalType.CAT);

        when(postRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        when(modelMapper.map(any(Post.class), eq(PostResponse.class))).thenReturn(postResponse);

        mockMvc.perform(get("/api/public/posts/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cute Cat"));
    }
}
