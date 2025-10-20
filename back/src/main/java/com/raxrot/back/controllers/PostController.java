package com.raxrot.back.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Post;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
@Slf4j
public class PostController {

    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @RequestPart("data") String requestString,
            @RequestPart("file") MultipartFile file) {
        log.info("📝 Create post request received | hasFile={}", file != null);
        try {
            PostRequest postRequest = objectMapper.readValue(requestString, PostRequest.class);
            log.info("📦 Parsed post data successfully | title='{}'", postRequest.getTitle());
            PostResponse response = postService.createPost(file, postRequest);
            log.info("✅ Post created successfully | postId={}", response.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse JSON for post data: {}", e.getMessage());
            throw new ApiException("Invalid JSON for post data", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/public/posts")
    public ResponseEntity<PostPageResponse> getAllPosts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        log.info("📄 Fetching all posts | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}", pageNumber, pageSize, sortBy, sortOrder);
        PostPageResponse response = postService.getAllPosts(pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts (page {})", response.getContent().size(), pageNumber);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/public/users/{username}/posts")
    public ResponseEntity<PostPageResponse> getAllPostsByUsername(
            @PathVariable String username,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        log.info("👤 Fetching posts by username='{}' | pageNumber={}, pageSize={}", username, pageNumber, pageSize);
        PostPageResponse response = postService.getPostsByUsername(username, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts for user '{}'", response.getContent().size(), username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/public/posts/animal/{type}")
    public ResponseEntity<PostPageResponse> getAllPostsByAnimalType(
            @PathVariable AnimalType type,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        log.info("🐾 Fetching posts by animal type='{}' | pageNumber={}, pageSize={}", type, pageNumber, pageSize);
        PostPageResponse response = postService.getPostsByAnimalType(type, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts for animal type='{}'", response.getContent().size(), type);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/public/posts/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        log.info("🔎 Fetching post details | postId={}", postId);
        PostResponse response = postService.getPostById(postId);
        log.info("✅ Post details retrieved successfully | postId={}", postId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        log.info("🗑️ Delete post request received | postId={}", postId);
        postService.deletePost(postId);
        log.info("✅ Post deleted successfully | postId={}", postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/feed/following")
    public ResponseEntity<PostPageResponse> getFollowingFeed(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer page,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer size,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        log.info("📰 Fetching following feed | page={}, size={}", page, size);
        PostPageResponse response = postService.getFollowingFeed(page, size, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts in following feed (page {})", response.getContent().size(), page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/posts/trending")
    public ResponseEntity<PostPageResponse> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("🔥 Fetching trending posts | page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewsCount").descending());
        Page<Post> postPage = postRepository.findAll(pageable);
        List<PostResponse> posts = postPage.getContent().stream()
                .map(p -> modelMapper.map(p, PostResponse.class))
                .toList();
        PostPageResponse response = new PostPageResponse(posts, page, size,
                postPage.getTotalPages(), postPage.getTotalElements(), postPage.isLast());
        log.info("✅ Retrieved {} trending posts (page {})", posts.size(), page);
        return ResponseEntity.ok(response);
    }
}
