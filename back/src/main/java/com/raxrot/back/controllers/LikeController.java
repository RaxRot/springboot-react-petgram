package com.raxrot.back.controllers;

import com.raxrot.back.services.LikeService;
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
        name = "Likes",
        description = "Endpoints for liking, unliking, and fetching total likes for posts."
)
public class LikeController {

    private final LikeService likeService;

    @Operation(
            summary = "Like a post",
            description = "Adds a like from the current authenticated user to the specified post.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Post liked successfully",
                            content = @Content(schema = @Schema(example = "{ \"likesCount\": 42 }"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> likePost(
            @Parameter(description = "ID of the post to like") @PathVariable Long postId
    ) {
        log.info("👍 Like request received for postId={}", postId);
        likeService.likePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ Post liked successfully | postId={} | totalLikes={}", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @Operation(
            summary = "Unlike a post",
            description = "Removes the current user's like from the specified post.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Post unliked successfully",
                            content = @Content(schema = @Schema(example = "{ \"likesCount\": 41 }"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> dislikePost(
            @Parameter(description = "ID of the post to unlike") @PathVariable Long postId
    ) {
        log.info("👎 Unlike request received for postId={}", postId);
        likeService.unlikePost(postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ Post unliked successfully | postId={} | totalLikes={}", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }

    @Operation(
            summary = "Get total likes for a post",
            description = "Returns the total number of likes for a given post. Public endpoint (no authentication required).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Likes fetched successfully",
                            content = @Content(schema = @Schema(example = "{ \"likesCount\": 12 }"))),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @GetMapping("/public/posts/{postId}/likes")
    public ResponseEntity<Map<String, Long>> getLikes(
            @Parameter(description = "ID of the post to fetch likes for") @PathVariable Long postId
    ) {
        log.info("📊 Fetching total likes for postId={}", postId);
        long likesCount = likeService.getLikesCount(postId);
        log.info("✅ postId={} has {} likes", postId, likesCount);
        return ResponseEntity.ok(Map.of("likesCount", likesCount));
    }
}
