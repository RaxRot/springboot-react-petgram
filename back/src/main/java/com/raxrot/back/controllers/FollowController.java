package com.raxrot.back.controllers;

import com.raxrot.back.services.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long userId) {
        followService.followUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId) {
        followService.unfollowUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/users/{userId}/followers/count")
    public ResponseEntity<Long> getFollowersCount(@PathVariable Long userId) {
        long followersCount = followService.getFollowersCount(userId);
        return ResponseEntity.ok(followersCount);
    }

    @GetMapping("/public/users/{userId}/following/count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long followingCount = followService.getFollowingCount(userId);
        return ResponseEntity.ok(followingCount);
    }

    @GetMapping("/users/{userId}/follow/state")
    public ResponseEntity<Map<String, Boolean>> followState(@PathVariable Long userId) {
        boolean following = followService.isFollowing(userId);
        return ResponseEntity.ok(Map.of("following", following));
    }
}
