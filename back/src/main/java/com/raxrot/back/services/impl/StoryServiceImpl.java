package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Story;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.StoryRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.StoryService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final AuthUtil authUtil;
    private final FileUploadService fileUploadService;
    private final StoryRepository storyRepository;
    private final ModelMapper modelMapper;

    private static final Duration TTL = Duration.ofHours(24);

    private StoryResponse toDto(Story s) {
        StoryResponse dto = modelMapper.map(s, StoryResponse.class);
        if (s.getUser() != null) {
            dto.setAuthorId(s.getUser().getUserId());
            dto.setAuthorUsername(s.getUser().getUserName());
        }
        return dto;
    }

    @Transactional
    @Override
    public StoryResponse create(MultipartFile file) {
        User me = authUtil.loggedInUser();
        if (me.isBanned()) {
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!contentType.startsWith("image/")) {
            throw new ApiException("Only image files are allowed", HttpStatus.BAD_REQUEST);
        }

        String url = fileUploadService.uploadFile(file);

        Story story = new Story();
        story.setUser(me);
        story.setImageUrl(url);
        story.setExpiresAt(LocalDateTime.now().plus(TTL));

        Story saved = storyRepository.save(story);
        return toDto(saved);
    }

    @Override
    public Page<StoryResponse> myStories(int page, int size) {
        User me = authUtil.loggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> pg = storyRepository.findAllByUser_UserIdAndExpiresAtAfter(me.getUserId(), LocalDateTime.now(), pageable);
        return pg.map(this::toDto);
    }

    @Override
    public Page<StoryResponse> followingStories(int page, int size) {
        User me = authUtil.loggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> pg = storyRepository.findFollowingStories(me.getUserId(), LocalDateTime.now(), pageable);
        return pg.map(this::toDto);
    }

    @Transactional
    @Override
    public void delete(Long storyId) {
        User me = authUtil.loggedInUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ApiException("Story not found", HttpStatus.NOT_FOUND));

        boolean isOwner = story.getUser().getUserId().equals(me.getUserId());
        boolean isAdmin = me.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            throw new ApiException("You are not allowed to delete this story", HttpStatus.FORBIDDEN);
        }

        try {
            fileUploadService.deleteFile(story.getImageUrl());
        } catch (Exception ignored) {}

        storyRepository.delete(story);
    }

    @Override
    public Page<StoryResponse> getAllStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return storyRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public StoryResponse viewStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ApiException("Story not found", HttpStatus.NOT_FOUND));

        User me = null;
        try {
            me = authUtil.loggedInUser();
        } catch (Exception ignored) {}

        if (me == null || !story.getUser().getUserId().equals(me.getUserId())) {
            story.setViewsCount(story.getViewsCount() + 1);
            storyRepository.save(story);
        }

        return toDto(story);
    }
}
