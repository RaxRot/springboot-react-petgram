package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId,@Valid @RequestBody CommentRequest req) {
        CommentResponse commentResponse=commentService.addComment(postId, req);
        return new ResponseEntity<>(commentResponse, HttpStatus.CREATED);
    }

    @GetMapping("/public/posts/{postId}/comments")
    public ResponseEntity<CommentPageResponse>getComments(
            @PathVariable Long postId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder){
        CommentPageResponse commentPageResponse=commentService.getComments(postId, pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(commentPageResponse, HttpStatus.OK);
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long commentId,@Valid @RequestBody CommentRequest req) {
        CommentResponse commentResponse=commentService.updateComment(commentId, req);
        return new ResponseEntity<>(commentResponse, HttpStatus.OK);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/comments")
    public ResponseEntity<CommentPageResponse> getAllComments(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        CommentPageResponse comments = commentService.getAllComments(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(comments);
    }

}
