package com.raxrot.back.controllers;

import com.raxrot.back.services.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> likePost(@PathVariable Long postId) {
        likeService.likePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> dislikePost(@PathVariable Long postId) {
        likeService.unlikePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @GetMapping("/public/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> getLikes(@PathVariable Long postId) {
        long likesCount = likeService.getLikesCount(postId);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }
}