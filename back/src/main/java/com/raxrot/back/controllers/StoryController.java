package com.raxrot.back.controllers;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.services.StoryService;
import lombok.RequiredArgsConstructor;
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
public class StoryController {

    private final StoryService storyService;

    @PostMapping(value = "/stories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryResponse> create(@RequestParam("file") MultipartFile file) {
        StoryResponse resp = storyService.create(file);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping("/stories/my")
    public ResponseEntity<Map<String, Object>> myStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<StoryResponse> pg = storyService.myStories(page, size);
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
        Page<StoryResponse> pg = storyService.followingStories(page, size);
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
        storyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/stories")
    public ResponseEntity<Map<String, Object>> publicStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<StoryResponse> pg = storyService.getAllStories(page, size);
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
        return ResponseEntity.ok(storyService.viewStory(id));
    }

}

