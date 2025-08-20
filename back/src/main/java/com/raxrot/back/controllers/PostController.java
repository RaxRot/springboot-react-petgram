package com.raxrot.back.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class PostController {
    private final PostService postService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @RequestPart("data") String requestString,
            @RequestPart("file") MultipartFile file) {

        try {
            PostRequest postRequest = objectMapper.readValue(requestString, PostRequest.class);
            PostResponse response = postService.createPost(file,postRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            throw new ApiException("Invalid JSON for post data", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/public/posts")
    public ResponseEntity<PostPageResponse>getAllPosts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder){
        PostPageResponse postPageResponse = postService.getAllPosts(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(postPageResponse, HttpStatus.OK);
    }

    @GetMapping("/public/users/{username}/posts")
    public ResponseEntity<PostPageResponse>getAllPostsByUsername(
            @PathVariable String username,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder){
        PostPageResponse postPageResponse = postService.getPostsByUsername(username,pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(postPageResponse, HttpStatus.OK);
    }

    @GetMapping("/public/posts/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        PostResponse postResponse = postService.getPostById(postId);
        return new ResponseEntity<>(postResponse, HttpStatus.OK);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void>deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
