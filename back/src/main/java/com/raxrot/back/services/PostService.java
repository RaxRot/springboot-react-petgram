package com.raxrot.back.services;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponse createPost(MultipartFile file, PostRequest postRequest, Authentication authentication);
    PostPageResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    PostResponse getPostById(Long postId);
    PostPageResponse getPostsByUsername(String username, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    void deletePost(Long postId, Authentication authentication);
}
