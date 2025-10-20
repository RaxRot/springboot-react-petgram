package com.raxrot.back.controllers;

import com.raxrot.back.services.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Follow System",
        description = "Endpoints for following and unfollowing users, as well as retrieving follower/following statistics."
)
public class FollowController {

    private final FollowService followService;

    @Operation(
            summary = "Follow a user",
            description = "Allows the current authenticated user to follow another user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully followed user"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<Void> follow(
            @Parameter(description = "ID of the user to follow") @PathVariable Long userId
    ) {
        log.info("🤝 Follow request received for userId={}", userId);
        followService.followUser(userId);
        log.info("✅ Successfully followed userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unfollow a user",
            description = "Allows the current authenticated user to unfollow another user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully unfollowed user"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<Void> unfollow(
            @Parameter(description = "ID of the user to unfollow") @PathVariable Long userId
    ) {
        log.info("🚫 Unfollow request received for userId={}", userId);
        followService.unfollowUser(userId);
        log.info("✅ Successfully unfollowed userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get follower count",
            description = "Returns the total number of followers for a given user. Public endpoint (no authentication required).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Follower count fetched successfully",
                            content = @Content(schema = @Schema(example = "42"))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/public/users/{userId}/followers/count")
    public ResponseEntity<Long> getFollowersCount(
            @Parameter(description = "User ID to fetch follower count for") @PathVariable Long userId
    ) {
        log.info("📊 Fetching followers count for userId={}", userId);
        long followersCount = followService.getFollowersCount(userId);
        log.info("✅ userId={} has {} followers", userId, followersCount);
        return ResponseEntity.ok(followersCount);
    }

    @Operation(
            summary = "Get following count",
            description = "Returns how many users the given user is following. Public endpoint (no authentication required).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Following count fetched successfully",
                            content = @Content(schema = @Schema(example = "17"))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/public/users/{userId}/following/count")
    public ResponseEntity<Long> getFollowingCount(
            @Parameter(description = "User ID to fetch following count for") @PathVariable Long userId
    ) {
        log.info("📊 Fetching following count for userId={}", userId);
        long followingCount = followService.getFollowingCount(userId);
        log.info("✅ userId={} is following {} users", userId, followingCount);
        return ResponseEntity.ok(followingCount);
    }

    @Operation(
            summary = "Check follow state",
            description = "Checks whether the current authenticated user is following the specified user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Follow state retrieved successfully",
                            content = @Content(schema = @Schema(example = "{ \"following\": true }"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/users/{userId}/follow/state")
    public ResponseEntity<Map<String, Boolean>> followState(
            @Parameter(description = "ID of the user to check follow state for") @PathVariable Long userId
    ) {
        log.info("🔎 Checking follow state for userId={}", userId);
        boolean following = followService.isFollowing(userId);
        log.info("✅ Follow state for userId={} → following={}", userId, following);
        return ResponseEntity.ok(Map.of("following", following));
    }
}
