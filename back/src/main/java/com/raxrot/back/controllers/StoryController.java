package com.raxrot.back.controllers;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.services.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class StoryController {

    private final StoryService storyService;

    @PostMapping(value = "/stories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryResponse> create(@RequestParam("file") MultipartFile file) {
        log.info("📸 Create story request received | hasFile={}", file != null);
        StoryResponse resp = storyService.create(file);
        log.info("✅ Story created successfully | storyId={}", resp.getId());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping("/stories/my")
    public ResponseEntity<Map<String, Object>> myStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🧾 Fetching current user's stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.myStories(page, size);
        log.info("✅ Retrieved {} stories for current user (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @GetMapping("/stories/following")
    public ResponseEntity<Map<String, Object>> following(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("👥 Fetching following users' stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.followingStories(page, size);
        log.info("✅ Retrieved {} stories from following users (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @DeleteMapping("/stories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("🗑️ Delete story request received | storyId={}", id);
        storyService.delete(id);
        log.info("✅ Story deleted successfully | storyId={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/stories")
    public ResponseEntity<Map<String, Object>> publicStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🌍 Fetching all public stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.getAllStories(page, size);
        log.info("✅ Retrieved {} public stories (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @GetMapping("/public/stories/{id}")
    public ResponseEntity<StoryResponse> view(@PathVariable Long id) {
        log.info("👁️ Viewing story | storyId={}", id);
        StoryResponse response = storyService.viewStory(id);
        log.info("✅ Story viewed successfully | storyId={}", id);
        return ResponseEntity.ok(response);
    }
}
