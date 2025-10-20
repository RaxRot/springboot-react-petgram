package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PublicUserResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
@Slf4j
public class PublicUserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicUser(@PathVariable String username) {
        log.info("🔎 Fetching public profile for username='{}'", username);
        PublicUserResponse userResponse = userService.getPublicUserByUsername(username);
        log.info("✅ Public profile retrieved successfully for username='{}'", username);
        return ResponseEntity.ok(userResponse);
    }

    // FIX LATER: ADD TO SERVICE
    @GetMapping("/username/{username}/id")
    public ResponseEntity<Long> getUserIdByUsername(@PathVariable String username) {
        log.info("🆔 Fetching userId by username='{}'", username);
        Long id = userRepository.findByUserName(username)
                .map(User::getUserId)
                .orElseThrow(() -> {
                    log.error("❌ User not found for username='{}'", username);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });
        log.info("✅ userId={} found for username='{}'", id, username);
        return ResponseEntity.ok(id);
    }
}
