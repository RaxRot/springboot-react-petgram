package com.raxrot.back.controllers;

import com.raxrot.back.services.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> likePost(@PathVariable Long postId) {
        log.info("👍 Like request received for postId={}", postId);
        likeService.likePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ Post liked successfully | postId={} | totalLikes={}", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> dislikePost(@PathVariable Long postId) {
        log.info("👎 Unlike request received for postId={}", postId);
        likeService.unlikePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ Post unliked successfully | postId={} | totalLikes={}", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @GetMapping("/public/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> getLikes(@PathVariable Long postId) {
        log.info("📊 Fetching total likes for postId={}", postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ postId={} has {} likes", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }
}
