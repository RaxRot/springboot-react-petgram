package com.raxrot.back.services;

import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;

public interface CommentService {
    CommentResponse addComment(Long postId, CommentRequest req);
    CommentPageResponse getComments(Long postId, Integer page, Integer size, String sortBy, String sortOrder);
    CommentResponse updateComment(Long commentId, CommentRequest req);
    void deleteComment(Long commentId);
    CommentPageResponse getAllComments(Integer page, Integer size, String sortBy, String sortOrder);
}
