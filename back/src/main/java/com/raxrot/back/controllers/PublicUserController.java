package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PublicUserResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Public Users",
        description = "Public endpoints for fetching user profiles and basic information by username."
)
public class PublicUserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(
            summary = "Get public user profile by username",
            description = "Returns a user's public profile (visible to everyone). Does not require authentication.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Public user profile retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PublicUserResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicUser(
            @Parameter(description = "Username of the user to fetch") @PathVariable String username
    ) {
        log.info("🔎 Fetching public profile for username='{}'", username);
        PublicUserResponse userResponse = userService.getPublicUserByUsername(username);
        log.info("✅ Public profile retrieved successfully for username='{}'", username);
        return ResponseEntity.ok(userResponse);
    }

    @Operation(
            summary = "Get user ID by username",
            description = "Fetches the internal user ID based on the provided username. Public endpoint, mostly used for internal references or front-end lookups.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User ID retrieved successfully",
                            content = @Content(schema = @Schema(example = "42"))
                    ),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/username/{username}/id")
    public ResponseEntity<Long> getUserIdByUsername(
            @Parameter(description = "Username to fetch corresponding user ID for") @PathVariable String username
    ) {
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
