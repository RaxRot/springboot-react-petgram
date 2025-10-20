package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.services.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest req) {
        String preview = req.getText() != null && req.getText().length() > 30
                ? req.getText().substring(0, 30) + "..."
                : req.getText();

        log.info("💬 Add comment request for postId={} | text preview='{}'", postId, preview);
        CommentResponse commentResponse = commentService.addComment(postId, req);
        log.info("✅ Comment added successfully to postId={} with commentId={}", postId, commentResponse.getId());
        return new ResponseEntity<>(commentResponse, HttpStatus.CREATED);
    }

    @GetMapping("/public/posts/{postId}/comments")
    public ResponseEntity<CommentPageResponse> getComments(
            @PathVariable Long postId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        log.info("🧾 Fetching comments for postId={} | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}",
                postId, pageNumber, pageSize, sortBy, sortOrder);
        CommentPageResponse commentPageResponse = commentService.getComments(postId, pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} comments for postId={}", commentPageResponse.getContent().size(), postId);
        return new ResponseEntity<>(commentPageResponse, HttpStatus.OK);
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long commentId, @Valid @RequestBody CommentRequest req) {
        String preview = req.getText() != null && req.getText().length() > 30
                ? req.getText().substring(0, 30) + "..."
                : req.getText();

        log.info("✏️ Update comment request for commentId={} | new text preview='{}'", commentId, preview);
        CommentResponse commentResponse = commentService.updateComment(commentId, req);
        log.info("✅ Comment updated successfully | commentId={}", commentId);
        return new ResponseEntity<>(commentResponse, HttpStatus.OK);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        log.info("🗑️ Delete comment request for commentId={}", commentId);
        commentService.deleteComment(commentId);
        log.info("✅ Comment deleted successfully | commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/comments")
    public ResponseEntity<CommentPageResponse> getAllComments(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.info("📋 Fetching all comments (admin) | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}",
                pageNumber, pageSize, sortBy, sortOrder);
        CommentPageResponse comments = commentService.getAllComments(pageNumber, pageSize, sortBy, sortOrder);
        log.info("✅ Retrieved {} total comments (admin view)", comments.getContent().size());
        return ResponseEntity.ok(comments);
    }
}
