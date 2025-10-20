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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Posts",
        description = "Endpoints for creating, fetching, filtering, and deleting posts."
)
public class PostController {

    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

    @Operation(
            summary = "Create a new post",
            description = "Creates a post with an image and text content. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Post created successfully",
                            content = @Content(schema = @Schema(implementation = PostResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON data or file format"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @Parameter(description = "Post data as JSON string") @RequestPart("data") String requestString,
            @Parameter(description = "Attached image file") @RequestPart("file") MultipartFile file
    ) {
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

    @Operation(
            summary = "Get all public posts",
            description = "Returns all public posts with pagination and sorting.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class)))
            }
    )
    @GetMapping("/public/posts")
    public ResponseEntity<PostPageResponse> getAllPosts(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @Parameter(description = "Sort field (default = createdAt)") @RequestParam(defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order asc/desc (default = desc)") @RequestParam(defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("📄 Fetching all posts | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}", pageNumber, pageSize, sortBy, sortOrder);
        PostPageResponse response = postService.getAllPosts(pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts (page {})", response.getContent().size(), pageNumber);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Get posts by username",
            description = "Returns posts created by a specific user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User posts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/public/users/{username}/posts")
    public ResponseEntity<PostPageResponse> getAllPostsByUsername(
            @Parameter(description = "Username to filter posts by") @PathVariable String username,
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @Parameter(description = "Sort field (default = createdAt)") @RequestParam(defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order asc/desc (default = desc)") @RequestParam(defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("👤 Fetching posts by username='{}' | pageNumber={}, pageSize={}", username, pageNumber, pageSize);
        PostPageResponse response = postService.getPostsByUsername(username, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts for user '{}'", response.getContent().size(), username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Get posts by animal type",
            description = "Fetches all posts filtered by animal type (e.g. CAT, DOG, BIRD).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class)))
            }
    )
    @GetMapping("/public/posts/animal/{type}")
    public ResponseEntity<PostPageResponse> getAllPostsByAnimalType(
            @Parameter(description = "Animal type (e.g. CAT, DOG, BIRD)") @PathVariable AnimalType type,
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @Parameter(description = "Sort field (default = createdAt)") @RequestParam(defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order asc/desc (default = desc)") @RequestParam(defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("🐾 Fetching posts by animal type='{}' | pageNumber={}, pageSize={}", type, pageNumber, pageSize);
        PostPageResponse response = postService.getPostsByAnimalType(type, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts for animal type='{}'", response.getContent().size(), type);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Get post by ID",
            description = "Fetches detailed information about a specific post. Public endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Post details retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @GetMapping("/public/posts/{postId}")
    public ResponseEntity<PostResponse> getPostById(
            @Parameter(description = "ID of the post to fetch") @PathVariable Long postId
    ) {
        log.info("🔎 Fetching post details | postId={}", postId);
        PostResponse response = postService.getPostById(postId);
        log.info("✅ Post details retrieved successfully | postId={}", postId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Delete post",
            description = "Deletes an existing post. Only the post owner or an admin can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not the post owner"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "ID of the post to delete") @PathVariable Long postId
    ) {
        log.info("🗑️ Delete post request received | postId={}", postId);
        postService.deletePost(postId);
        log.info("✅ Post deleted successfully | postId={}", postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get following feed",
            description = "Retrieves posts from users that the authenticated user is following.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Following feed retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/posts/feed/following")
    public ResponseEntity<PostPageResponse> getFollowingFeed(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) Integer page,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = AppConstants.PAGE_SIZE) Integer size,
            @Parameter(description = "Sort field (default = createdAt)") @RequestParam(defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order asc/desc (default = desc)") @RequestParam(defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("📰 Fetching following feed | page={}, size={}", page, size);
        PostPageResponse response = postService.getFollowingFeed(page, size, sortBy, sortOrder);
        log.info("✅ Retrieved {} posts in following feed (page {})", response.getContent().size(), page);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get trending posts",
            description = "Retrieves trending posts sorted by most views. Public endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trending posts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class)))
            }
    )
    @GetMapping("/public/posts/trending")
    public ResponseEntity<PostPageResponse> getTrending(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = "10") int size
    ) {
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
