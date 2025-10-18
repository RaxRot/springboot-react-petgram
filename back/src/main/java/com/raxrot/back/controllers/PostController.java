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
public class PostController {
    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

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
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
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

    @GetMapping("/public/posts/animal/{type}")
    public ResponseEntity<PostPageResponse>getAllPostsByAnimalType(
            @PathVariable AnimalType type,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder){
        PostPageResponse postPageResponse=postService.getPostsByAnimalType(type,pageNumber,pageSize,sortBy,sortOrder);
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

    @GetMapping("/posts/feed/following")
    public ResponseEntity<PostPageResponse> getFollowingFeed(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer page,
            @RequestParam(name = "pageSize",   defaultValue = AppConstants.PAGE_SIZE)   Integer size,
            @RequestParam(name = "sortBy",     defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @RequestParam(name = "sortOrder",  defaultValue = AppConstants.SORT_DIR)   String sortOrder
    ) {
        return ResponseEntity.ok(postService.getFollowingFeed(page, size, sortBy, sortOrder));
    }

    @GetMapping("/public/posts/trending")
    public ResponseEntity<PostPageResponse> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewsCount").descending());
        Page<Post> postPage = postRepository.findAll(pageable);
        List<PostResponse> posts = postPage.getContent().stream()
                .map(p -> modelMapper.map(p, PostResponse.class))
                .toList();

        PostPageResponse response = new PostPageResponse(posts, page, size,
                postPage.getTotalPages(), postPage.getTotalElements(), postPage.isLast());
        return ResponseEntity.ok(response);
    }

}
