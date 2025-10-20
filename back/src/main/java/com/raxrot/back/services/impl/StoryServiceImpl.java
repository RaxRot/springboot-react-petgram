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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        log.info("User '{}' attempting to create a new story", me.getUserName());

        if (me.isBanned()) {
            log.warn("Banned user '{}' tried to create a story", me.getUserName());
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }

        if (file == null || file.isEmpty()) {
            log.warn("User '{}' attempted to upload an empty file for story", me.getUserName());
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!contentType.startsWith("image/")) {
            log.warn("User '{}' tried to upload invalid file type '{}' for story", me.getUserName(), contentType);
            throw new ApiException("Only image files are allowed", HttpStatus.BAD_REQUEST);
        }

        String url = fileUploadService.uploadFile(file);
        log.info("File uploaded successfully for story by '{}': {}", me.getUserName(), url);

        Story story = new Story();
        story.setUser(me);
        story.setImageUrl(url);
        story.setExpiresAt(LocalDateTime.now().plus(TTL));

        Story saved = storyRepository.save(story);
        log.info("Story created successfully by user '{}' with ID {}", me.getUserName(), saved.getId());
        return toDto(saved);
    }

    @Override
    public Page<StoryResponse> myStories(int page, int size) {
        User me = authUtil.loggedInUser();
        log.info("Fetching stories for user '{}' (page={}, size={})", me.getUserName(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> pg = storyRepository.findAllByUser_UserIdAndExpiresAtAfter(me.getUserId(), LocalDateTime.now(), pageable);

        log.info("Fetched {} active stories for user '{}'", pg.getTotalElements(), me.getUserName());
        return pg.map(this::toDto);
    }

    @Override
    public Page<StoryResponse> followingStories(int page, int size) {
        User me = authUtil.loggedInUser();
        log.info("Fetching following stories for user '{}' (page={}, size={})", me.getUserName(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> pg = storyRepository.findFollowingStories(me.getUserId(), LocalDateTime.now(), pageable);

        log.info("Fetched {} following stories for user '{}'", pg.getTotalElements(), me.getUserName());
        return pg.map(this::toDto);
    }

    @Transactional
    @Override
    public void delete(Long storyId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to delete story ID {}", me.getUserName(), storyId);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> {
                    log.error("Story with ID {} not found for deletion by '{}'", storyId, me.getUserName());
                    return new ApiException("Story not found", HttpStatus.NOT_FOUND);
                });

        boolean isOwner = story.getUser().getUserId().equals(me.getUserId());
        boolean isAdmin = me.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            log.warn("User '{}' attempted to delete someone else's story (ID {})", me.getUserName(), storyId);
            throw new ApiException("You are not allowed to delete this story", HttpStatus.FORBIDDEN);
        }

        try {
            fileUploadService.deleteFile(story.getImageUrl());
            log.info("Deleted file for story ID {}", storyId);
        } catch (Exception e) {
            log.warn("Failed to delete file for story ID {}: {}", storyId, e.getMessage());
        }

        storyRepository.delete(story);
        log.info("Story ID {} successfully deleted by user '{}'", storyId, me.getUserName());
    }

    @Override
    public Page<StoryResponse> getAllStories(int page, int size) {
        log.info("Fetching all stories (page={}, size={})", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StoryResponse> pg = storyRepository.findAll(pageable).map(this::toDto);

        log.info("Fetched {} total stories", pg.getTotalElements());
        return pg;
    }

    @Transactional
    @Override
    public StoryResponse viewStory(Long id) {
        log.info("Fetching story ID {} for viewing", id);

        Story story = storyRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Story with ID {} not found", id);
                    return new ApiException("Story not found", HttpStatus.NOT_FOUND);
                });

        User me = null;
        try {
            me = authUtil.loggedInUser();
        } catch (Exception ignored) {}

        if (me == null || !story.getUser().getUserId().equals(me.getUserId())) {
            story.setViewsCount(story.getViewsCount() + 1);
            storyRepository.save(story);
            log.debug("Increased views count for story ID {} to {}", id, story.getViewsCount());
        }

        log.info("Story ID {} viewed successfully", id);
        return toDto(story);
    }
}
