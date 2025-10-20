package com.raxrot.back.controllers;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.services.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Stories",
        description = "Endpoints for creating, viewing, deleting, and fetching user and public stories."
)
public class StoryController {

    private final StoryService storyService;

    @Operation(
            summary = "Create a new story",
            description = "Creates a new story with an uploaded media file (image or video). Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Story created successfully",
                            content = @Content(schema = @Schema(implementation = StoryResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing file"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PostMapping(value = "/stories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryResponse> create(
            @Parameter(description = "Media file for the story") @RequestParam("file") MultipartFile file
    ) {
        log.info("📸 Create story request received | hasFile={}", file != null);
        StoryResponse resp = storyService.create(file);
        log.info("✅ Story created successfully | storyId={}", resp.getId());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Get current user's stories",
            description = "Returns a paginated list of stories created by the current authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User stories retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/stories/my")
    public ResponseEntity<Map<String, Object>> myStories(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 20)") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🧾 Fetching current user's stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.myStories(page, size);
        log.info("✅ Retrieved {} stories for current user (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @Operation(
            summary = "Get stories from followed users",
            description = "Retrieves recent stories from users that the current user follows. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Following users' stories retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/stories/following")
    public ResponseEntity<Map<String, Object>> following(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 20)") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("👥 Fetching following users' stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.followingStories(page, size);
        log.info("✅ Retrieved {} stories from following users (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @Operation(
            summary = "Delete a story",
            description = "Deletes a story owned by the authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Story deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not the story owner"),
                    @ApiResponse(responseCode = "404", description = "Story not found")
            }
    )
    @DeleteMapping("/stories/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the story to delete") @PathVariable Long id
    ) {
        log.info("🗑️ Delete story request received | storyId={}", id);
        storyService.delete(id);
        log.info("✅ Story deleted successfully | storyId={}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get all public stories",
            description = "Retrieves a paginated list of all public stories across the platform.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Public stories retrieved successfully")
            }
    )
    @GetMapping("/public/stories")
    public ResponseEntity<Map<String, Object>> publicStories(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 20)") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🌍 Fetching all public stories | page={}, size={}", page, size);
        Page<StoryResponse> pg = storyService.getAllStories(page, size);
        log.info("✅ Retrieved {} public stories (page {})", pg.getContent().size(), page);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @Operation(
            summary = "View a public story by ID",
            description = "Returns story details and registers a view for analytics. Public endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Story retrieved successfully",
                            content = @Content(schema = @Schema(implementation = StoryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Story not found")
            }
    )
    @GetMapping("/public/stories/{id}")
    public ResponseEntity<StoryResponse> view(
            @Parameter(description = "ID of the story to view") @PathVariable Long id
    ) {
        log.info("👁️ Viewing story | storyId={}", id);
        StoryResponse response = storyService.viewStory(id);
        log.info("✅ Story viewed successfully | storyId={}", id);
        return ResponseEntity.ok(response);
    }
}
