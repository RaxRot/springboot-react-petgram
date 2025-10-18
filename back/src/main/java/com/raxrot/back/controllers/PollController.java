package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;
import com.raxrot.back.services.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> createPoll(
            @PathVariable Long postId,
            @Valid @RequestBody PollRequest req) {
        PollResponse resp = pollService.createPoll(postId, req);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping("/posts/{postId}/polls")
    public ResponseEntity<PollResponse> getPoll(@PathVariable Long postId) {
        return ResponseEntity.ok(pollService.getPoll(postId));
    }

    @PostMapping("/polls/{pollId}/vote/{optionId}")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long pollId,
            @PathVariable Long optionId) {
        PollResponse resp = pollService.vote(pollId, optionId);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/posts/{postId}/polls")
    public ResponseEntity<Void> deletePoll(@PathVariable Long postId) {
        pollService.deletePoll(postId);
        return ResponseEntity.noContent().build();
    }

}
