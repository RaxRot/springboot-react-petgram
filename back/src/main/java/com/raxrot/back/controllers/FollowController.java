package com.raxrot.back.controllers;

import com.raxrot.back.services.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowService followService;

    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long userId) {
        log.info("🤝 Follow request received for userId={}", userId);
        followService.followUser(userId);
        log.info("✅ Successfully followed userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId) {
        log.info("🚫 Unfollow request received for userId={}", userId);
        followService.unfollowUser(userId);
        log.info("✅ Successfully unfollowed userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/users/{userId}/followers/count")
    public ResponseEntity<Long> getFollowersCount(@PathVariable Long userId) {
        log.info("📊 Fetching followers count for userId={}", userId);
        long followersCount = followService.getFollowersCount(userId);
        log.info("✅ userId={} has {} followers", userId, followersCount);
        return ResponseEntity.ok(followersCount);
    }

    @GetMapping("/public/users/{userId}/following/count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        log.info("📊 Fetching following count for userId={}", userId);
        long followingCount = followService.getFollowingCount(userId);
        log.info("✅ userId={} is following {} users", userId, followingCount);
        return ResponseEntity.ok(followingCount);
    }

    @GetMapping("/users/{userId}/follow/state")
    public ResponseEntity<Map<String, Boolean>> followState(@PathVariable Long userId) {
        log.info("🔎 Checking follow state for userId={}", userId);
        boolean following = followService.isFollowing(userId);
        log.info("✅ Follow state for userId={} → following={}", userId, following);
        return ResponseEntity.ok(Map.of("following", following));
    }
}
