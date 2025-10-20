package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;
import com.raxrot.back.services.PollService;
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
public class PollController {

    private final PollService pollService;

    @PostMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> createPoll(
            @PathVariable Long postId,
            @Valid @RequestBody PollRequest req
    ) {
        log.info("🗳️ Create poll request received | postId={}", postId);
        PollResponse resp = pollService.createPoll(postId, req);
        log.info("✅ Poll created successfully | postId={} | pollId={}", postId, resp.getPollId());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> getPoll(@PathVariable Long postId) {
        log.info("🔎 Fetching poll for postId={}", postId);
        PollResponse poll = pollService.getPoll(postId);
        log.info("✅ Poll retrieved successfully | postId={} | pollId={}", postId, poll.getPollId());
        return ResponseEntity.ok(poll);
    }

    @PostMapping("/polls/{pollId}/vote/{optionId}")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long pollId,
            @PathVariable Long optionId
    ) {
        log.info("🗳️ Vote request received | pollId={} | optionId={}", pollId, optionId);
        PollResponse resp = pollService.vote(pollId, optionId);
        log.info("✅ Vote registered successfully | pollId={} | voted={}", pollId, resp.isVoted());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/posts/{postId}/polls")
    public ResponseEntity<Void> deletePoll(@PathVariable Long postId) {
        log.info("🗑️ Delete poll request received | postId={}", postId);
        pollService.deletePoll(postId);
        log.info("✅ Poll deleted successfully | postId={}", postId);
        return ResponseEntity.noContent().build();
    }
}
