package com.raxrot.back.services;

import com.raxrot.back.dtos.StoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface StoryService {
    StoryResponse create(MultipartFile file);
    Page<StoryResponse> myStories(int page, int size);
    Page<StoryResponse> followingStories(int page, int size);
    void delete(Long storyId);

    Page<StoryResponse> getAllStories(int page, int size);
    StoryResponse viewStory(Long id);
}
