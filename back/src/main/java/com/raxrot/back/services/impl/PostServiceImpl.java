package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

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
        if (user.isBanned()) {
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }
        if (file == null || file.isEmpty())
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        if (file.getContentType() == null || !file.getContentType().startsWith("image/"))
            throw new ApiException("Only image files are allowed", HttpStatus.BAD_REQUEST);

        String imageUrl = fileUploadService.uploadFile(file);
        Post post = modelMapper.map(postRequest, Post.class);
        post.setUser(user);
        post.setImageUrl(imageUrl);
        Post savedPost = postRepository.save(post);
        return modelMapper.map(savedPost, PostResponse.class);
    }

    @Override
    public PostPageResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> postPage = postRepository.findAll(pageable);

        List<PostResponse> postResponses = postPage.getContent().stream()
                .map(post -> modelMapper.map(post, PostResponse.class))
                .collect(Collectors.toList());

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
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> postPage = postRepository.findAllByUser_UserName(username, pageable);

        List<PostResponse> postResponses = postPage.getContent().stream()
                .map(post -> modelMapper.map(post, PostResponse.class))
                .collect(Collectors.toList());

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
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));
        return modelMapper.map(post, PostResponse.class);
    }

    @Transactional
    @Override
    public void deletePost(Long postId) {
        User me = authUtil.loggedInUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));

        boolean isOwner = post.getUser().getUserId().equals(me.getUserId());
        boolean isAdmin = me.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            throw new ApiException("You are not allowed to delete this post", HttpStatus.FORBIDDEN);
        }

        String img = post.getImageUrl();
        if (img != null && !img.isBlank()) {
            try {
                fileUploadService.deleteFile(img);
            } catch (Exception ignored) {

            }
        }
        postRepository.delete(post);
    }
}
