package com.raxrot.back.controllers;

import com.raxrot.back.dtos.ChangePasswordRequest;
import com.raxrot.back.dtos.UpdateUsernameRequest;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.services.UserService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "User Profile",
        description = "Endpoints for managing user profile data, username updates, passwords, and profile pictures."
)
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Operation(
            summary = "Upload or update profile image",
            description = "Uploads a new profile image for the authenticated user. Image is saved and associated with their profile.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile image uploaded successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing file"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PatchMapping(value = "/uploadimg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadImg(
            @Parameter(description = "Profile picture file") @RequestParam("file") MultipartFile file
    ) {
        log.info("🖼️ Profile image upload request received | fileName={}", file.getOriginalFilename());
        UserResponse response = userService.uploadImgProfilePic(file);
        log.info("✅ Profile image updated successfully | userId={} | username='{}'",
                response.getId(), response.getUserName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user by username",
            description = "Fetches a user's public profile information by their username (used for search results).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = UserResponseForSearch.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseForSearch> getUserByUsername(
            @Parameter(description = "Username to search for") @PathVariable String username
    ) {
        log.info("🔎 Fetching user by username='{}'", username);
        UserResponseForSearch userResponse = userService.getUserByUsername(username);
        log.info("✅ User found | username='{}' | profilePic={}",
                userResponse.getUserName(), userResponse.getProfilePic());
        return ResponseEntity.ok(userResponse);
    }

    @Operation(
            summary = "Update current user's username",
            description = "Updates the username of the authenticated user. A new JWT cookie is automatically issued with the updated identity.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Username updated successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid username or already taken"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PatchMapping("/username")
    public ResponseEntity<UserResponse> updateUsername(
            @Valid @RequestBody UpdateUsernameRequest req
    ) {
        log.info("✏️ Username update request received | newUsername='{}'", req.getNewUsername());
        UserResponse resp = userService.updateUsername(req);
        log.info("✅ Username updated successfully | userId={} | newUsername='{}'",
                resp.getId(), resp.getUserName());

        // Refresh authentication context
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl old = (UserDetailsImpl) current.getPrincipal();
        log.debug("🔐 Refreshing SecurityContext for userId={}", old.getId());

        UserDetailsImpl refreshed = new UserDetailsImpl(
                old.getId(),
                resp.getUserName(),
                resp.getEmail(),
                old.getPassword(),
                old.getAuthorities()
        );

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(refreshed, current.getCredentials(), refreshed.getAuthorities());
        newAuth.setDetails(current.getDetails());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        ResponseCookie jwtCookie = jwtUtils.getJwtCookie(refreshed);
        log.info("🍪 New JWT cookie generated for user='{}'", refreshed.getUsername());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(resp);
    }

    @Operation(
            summary = "Change password",
            description = "Updates the current user's password and clears the JWT cookie for security. User must log in again.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Password updated successfully and JWT cleared"),
                    @ApiResponse(responseCode = "400", description = "Invalid current password or weak new password"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        log.info("🔒 Password update request received for current user");
        userService.updatePassword(req);
        ResponseCookie clean = jwtUtils.getCleanJwtCookie();
        log.info("✅ Password updated and JWT cookie cleared");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clean.toString())
                .build();
    }
}
