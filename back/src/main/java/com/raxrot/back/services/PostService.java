package com.raxrot.back.services;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.enums.AnimalType;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponse createPost(MultipartFile file, PostRequest postRequest);
    PostPageResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    PostResponse getPostById(Long postId);
    PostPageResponse getPostsByUsername(String username, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    void deletePost(Long postId);
    PostPageResponse getPostsByAnimalType(AnimalType type, int page, int size, String sortBy, String sortOrder);
    PostPageResponse getFollowingFeed(int page, int size, String sortBy, String sortOrder);
}
