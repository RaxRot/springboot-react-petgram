package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;
import com.raxrot.back.services.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Polls",
        description = "Endpoints for managing polls — create, vote, view, and delete polls for posts."
)
public class PollController {

    private final PollService pollService;

    @Operation(
            summary = "Create a poll for a post",
            description = "Creates a new poll attached to a specific post. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Poll created successfully",
                            content = @Content(schema = @Schema(implementation = PollResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @PostMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> createPoll(
            @Parameter(description = "ID of the post to attach the poll to") @PathVariable Long postId,
            @Valid @RequestBody PollRequest req
    ) {
        log.info("🗳️ Create poll request received | postId={}", postId);
        PollResponse resp = pollService.createPoll(postId, req);
        log.info("✅ Poll created successfully | postId={} | pollId={}", postId, resp.getPollId());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Get poll for a post",
            description = "Retrieves poll details for a specific post. Publicly accessible.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Poll retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PollResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Poll not found for this post")
            }
    )
    @GetMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> getPoll(
            @Parameter(description = "ID of the post whose poll is requested") @PathVariable Long postId
    ) {
        log.info("🔎 Fetching poll for postId={}", postId);
        PollResponse poll = pollService.getPoll(postId);
        log.info("✅ Poll retrieved successfully | postId={} | pollId={}", postId, poll.getPollId());
        return ResponseEntity.ok(poll);
    }

    @Operation(
            summary = "Vote in a poll",
            description = "Registers a user's vote for a specific option in a poll. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Vote registered successfully",
                            content = @Content(schema = @Schema(implementation = PollResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid poll or option ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Poll or option not found")
            }
    )
    @PostMapping("/polls/{pollId}/vote/{optionId}")
    public ResponseEntity<PollResponse> vote(
            @Parameter(description = "ID of the poll to vote in") @PathVariable Long pollId,
            @Parameter(description = "ID of the selected option") @PathVariable Long optionId
    ) {
        log.info("🗳️ Vote request received | pollId={} | optionId={}", pollId, optionId);
        PollResponse resp = pollService.vote(pollId, optionId);
        log.info("✅ Vote registered successfully | pollId={} | voted={}", pollId, resp.isVoted());
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Delete poll for a post",
            description = "Deletes the poll associated with a given post. Only the post owner or an admin can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Poll deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not the poll owner"),
                    @ApiResponse(responseCode = "404", description = "Poll not found")
            }
    )
    @DeleteMapping("/posts/{postId}/polls")
    public ResponseEntity<Void> deletePoll(
            @Parameter(description = "ID of the post whose poll should be deleted") @PathVariable Long postId
    ) {
        log.info("🗑️ Delete poll request received | postId={}", postId);
        pollService.deletePoll(postId);
        log.info("✅ Poll deleted successfully | postId={}", postId);
        return ResponseEntity.noContent().build();
    }
}
