package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.PostService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    @Transactional
    @Override
    public PostResponse createPost(MultipartFile file, PostRequest postRequest) {
        User user = authUtil.loggedInUser();
        log.info("User '{}' attempting to create a new post", user.getUserName());

        if (user.isBanned()) {
            log.warn("Banned user '{}' tried to create a post", user.getUserName());
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }

        if (file == null || file.isEmpty()) {
            log.warn("User '{}' attempted to create a post without a file", user.getUserName());
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            log.warn("User '{}' tried to upload invalid file type '{}'", user.getUserName(), file.getContentType());
            throw new ApiException("Only image files are allowed", HttpStatus.BAD_REQUEST);
        }

        String imageUrl = fileUploadService.uploadFile(file);
        log.info("Image uploaded successfully for user '{}': {}", user.getUserName(), imageUrl);

        Post post = modelMapper.map(postRequest, Post.class);
        post.setUser(user);
        post.setImageUrl(imageUrl);
        Post savedPost = postRepository.save(post);

        log.info("Post created successfully by user '{}', post ID {}", user.getUserName(), savedPost.getId());
        return modelMapper.map(savedPost, PostResponse.class);
    }

    @Override
    public PostPageResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching all posts (page={}, size={}, sortBy={}, order={})", pageNumber, pageSize, sortBy, sortOrder);

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> postPage = postRepository.findAll(pageable);

        List<PostResponse> postResponses = postPage.getContent().stream()
                .map(post -> modelMapper.map(post, PostResponse.class))
                .collect(Collectors.toList());

        log.info("Fetched {} total posts", postPage.getTotalElements());

        PostPageResponse response = new PostPageResponse();
        response.setContent(postResponses);
        response.setPageNumber(postPage.getNumber());
        response.setPageSize(postPage.getSize());
        response.setTotalElements(postPage.getTotalElements());
        response.setTotalPages(postPage.getTotalPages());
        response.setLastPage(postPage.isLast());
        return response;
    }

    @Override
    public PostPageResponse getPostsByUsername(String username, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching posts for username='{}' (page={}, size={})", username, pageNumber, pageSize);

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> postPage = postRepository.findAllByUser_UserName(username, pageable);

        List<PostResponse> postResponses = postPage.getContent().stream()
                .map(post -> modelMapper.map(post, PostResponse.class))
                .collect(Collectors.toList());

        log.info("Fetched {} posts for user '{}'", postPage.getTotalElements(), username);

        PostPageResponse response = new PostPageResponse();
        response.setContent(postResponses);
        response.setPageNumber(postPage.getNumber());
        response.setPageSize(postPage.getSize());
        response.setTotalElements(postPage.getTotalElements());
        response.setTotalPages(postPage.getTotalPages());
        response.setLastPage(postPage.isLast());
        return response;
    }

    @Transactional
    @Override
    public PostResponse getPostById(Long postId) {
        log.info("Fetching post by ID {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        User me = null;
        try {
            me = authUtil.loggedInUser();
        } catch (Exception ignored) {}

        if (me == null || !post.getUser().getUserId().equals(me.getUserId())) {
            post.setViewsCount(post.getViewsCount() + 1);
            postRepository.save(post);
            log.debug("Increased view count for post ID {}. Current views: {}", postId, post.getViewsCount());
        }

        log.info("Fetched post ID {} successfully", postId);
        return modelMapper.map(post, PostResponse.class);
    }

    @Transactional
    @Override
    public void deletePost(Long postId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to delete post ID {}", me.getUserName(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found for deletion", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        boolean isOwner = post.getUser().getUserId().equals(me.getUserId());
        boolean isAdmin = me.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            log.warn("User '{}' attempted to delete another user's post (ID {})", me.getUserName(), postId);
            throw new ApiException("You are not allowed to delete this post", HttpStatus.FORBIDDEN);
        }

        String img = post.getImageUrl();
        if (img != null && !img.isBlank()) {
            try {
                fileUploadService.deleteFile(img);
                log.info("Deleted image for post ID {}", postId);
            } catch (Exception e) {
                log.warn("Failed to delete image for post ID {}: {}", postId, e.getMessage());
            }
        }

        postRepository.delete(post);
        log.info("Post ID {} successfully deleted by user '{}'", postId, me.getUserName());
    }

    @Override
    public PostPageResponse getPostsByAnimalType(AnimalType type, int page, int size, String sortBy, String sortOrder) {
        log.info("Fetching posts by animal type '{}' (page={}, size={})", type, page, size);

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Post> postPage = postRepository.findAllByAnimalType(type, pageable);

        List<PostResponse> postResponses = postPage.getContent().stream()
                .map(post -> modelMapper.map(post, PostResponse.class))
                .collect(Collectors.toList());

        log.info("Fetched {} posts of type '{}'", postPage.getTotalElements(), type);

        PostPageResponse response = new PostPageResponse();
        response.setContent(postResponses);
        response.setPageNumber(postPage.getNumber());
        response.setPageSize(postPage.getSize());
        response.setTotalElements(postPage.getTotalElements());
        response.setTotalPages(postPage.getTotalPages());
        response.setLastPage(postPage.isLast());
        return response;
    }

    @Override
    public PostPageResponse getFollowingFeed(int page, int size, String sortBy, String sortOrder) {
        User me = authUtil.loggedInUser();
        log.info("Fetching following feed for user '{}' (page={}, size={})", me.getUserName(), page, size);

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Post> postPage = postRepository.findFollowingFeed(me.getUserId(), pageable);

        List<PostResponse> content = postPage.getContent().stream()
                .map(p -> modelMapper.map(p, PostResponse.class))
                .toList();

        log.info("Fetched {} posts in following feed for user '{}'", postPage.getTotalElements(), me.getUserName());

        PostPageResponse resp = new PostPageResponse();
        resp.setContent(content);
        resp.setPageNumber(postPage.getNumber());
        resp.setPageSize(postPage.getSize());
        resp.setTotalElements(postPage.getTotalElements());
        resp.setTotalPages(postPage.getTotalPages());
        resp.setLastPage(postPage.isLast());
        return resp;
    }
}
