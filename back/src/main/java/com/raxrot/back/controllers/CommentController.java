package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.services.CommentService;
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
        name = "Comments",
        description = "Endpoints for creating, viewing, updating, and deleting post comments."
)
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "Add a comment to a post",
            description = "Creates a new comment under the specified post. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Comment successfully created",
                            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "ID of the post to comment on") @PathVariable Long postId,
            @Valid @RequestBody CommentRequest req
    ) {
        String preview = req.getText() != null && req.getText().length() > 30
                ? req.getText().substring(0, 30) + "..."
                : req.getText();

        log.info("💬 Add comment request for postId={} | text preview='{}'", postId, preview);
        CommentResponse commentResponse = commentService.addComment(postId, req);
        log.info("✅ Comment added successfully to postId={} with commentId={}", postId, commentResponse.getId());
        return new ResponseEntity<>(commentResponse, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Get public comments for a post",
            description = "Returns a paginated list of comments for a public post (no authentication required).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Comments successfully retrieved",
                            content = @Content(schema = @Schema(implementation = CommentPageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @GetMapping("/public/posts/{postId}/comments")
    public ResponseEntity<CommentPageResponse> getComments(
            @Parameter(description = "Post ID to fetch comments for") @PathVariable Long postId,
            @Parameter(description = "Page number (default = 0)")
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)")
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @Parameter(description = "Sort by field (default = createdAt)")
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order asc/desc (default = asc)")
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("🧾 Fetching comments for postId={} | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}",
                postId, pageNumber, pageSize, sortBy, sortOrder);
        CommentPageResponse commentPageResponse =
                commentService.getComments(postId, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} comments for postId={}", commentPageResponse.getContent().size(), postId);
        return new ResponseEntity<>(commentPageResponse, HttpStatus.OK);
    }

    @Operation(
            summary = "Update a comment",
            description = "Edits an existing comment. Only the comment's author or an admin can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid update request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not comment owner"),
                    @ApiResponse(responseCode = "404", description = "Comment not found")
            }
    )
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID of the comment to update") @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest req
    ) {
        String preview = req.getText() != null && req.getText().length() > 30
                ? req.getText().substring(0, 30) + "..."
                : req.getText();

        log.info("✏️ Update comment request for commentId={} | new text preview='{}'", commentId, preview);
        CommentResponse commentResponse = commentService.updateComment(commentId, req);
        log.info("✅ Comment updated successfully | commentId={}", commentId);
        return new ResponseEntity<>(commentResponse, HttpStatus.OK);
    }

    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment. Only the comment's author or an admin can delete it.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not comment owner"),
                    @ApiResponse(responseCode = "404", description = "Comment not found")
            }
    )
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID of the comment to delete") @PathVariable Long commentId
    ) {
        log.info("🗑️ Delete comment request for commentId={}", commentId);
        commentService.deleteComment(commentId);
        log.info("✅ Comment deleted successfully | commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get all comments (admin)",
            description = "Retrieves all comments across the platform. Accessible only by administrators.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "All comments successfully retrieved",
                            content = @Content(schema = @Schema(implementation = CommentPageResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden — not admin")
            }
    )
    @GetMapping("/admin/comments")
    public ResponseEntity<CommentPageResponse> getAllComments(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Page size (default = 20)") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "Sort by field (default = createdAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort order asc/desc (default = desc)") @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.info("📋 Fetching all comments (admin) | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}",
                pageNumber, pageSize, sortBy, sortOrder);
        CommentPageResponse comments = commentService.getAllComments(pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} total comments (admin view)", comments.getContent().size());
        return ResponseEntity.ok(comments);
    }
}
