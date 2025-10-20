package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Comment;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.CommentService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public CommentResponse addComment(Long postId, CommentRequest req) {
        User user = authUtil.loggedInUser();
        log.info("User '{}' attempting to add comment to post ID {}", user.getUserName(), postId);

        if (user.isBanned()) {
            log.warn("Banned user '{}' attempted to comment on post ID {}", user.getUserName(), postId);
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while user '{}' tried to comment", postId, user.getUserName());
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        Comment comment = modelMapper.map(req, Comment.class);
        comment.setPost(post);
        comment.setAuthor(user);
        Comment savedComment = commentRepository.save(comment);

        log.info("User '{}' added comment (ID={}) to post ID {}", user.getUserName(), savedComment.getId(), postId);
        return modelMapper.map(savedComment, CommentResponse.class);
    }

    @Override
    public CommentPageResponse getComments(Long postId, Integer page, Integer size, String sortBy, String sortOrder) {
        log.info("Fetching comments for post ID {} (page={}, size={}, sortBy={}, order={})", postId, page, size, sortBy, sortOrder);

        if (!postRepository.existsById(postId)) {
            log.error("Attempt to fetch comments for non-existing post ID {}", postId);
            throw new ApiException("Post not found", HttpStatus.NOT_FOUND);
        }

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> commentPage = commentRepository.findAllByPost_Id(postId, pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(c -> modelMapper.map(c, CommentResponse.class))
                .toList();

        CommentPageResponse resp = new CommentPageResponse();
        resp.setContent(comments);
        resp.setPageNumber(commentPage.getNumber());
        resp.setPageSize(commentPage.getSize());
        resp.setTotalElements(commentPage.getTotalElements());
        resp.setTotalPages(commentPage.getTotalPages());
        resp.setLastPage(commentPage.isLast());

        log.info("Fetched {} comments for post ID {}", commentPage.getTotalElements(), postId);
        return resp;
    }

    @Transactional
    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest req) {
        User user = authUtil.loggedInUser();
        log.info("User '{}' attempting to update comment ID {}", user.getUserName(), commentId);

        if (user.isBanned()) {
            log.warn("Banned user '{}' attempted to update comment ID {}", user.getUserName(), commentId);
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment with ID {} not found for update", commentId);
                    return new ApiException("Comment not found", HttpStatus.NOT_FOUND);
                });

        if (!comment.getAuthor().getUserId().equals(user.getUserId())) {
            log.warn("User '{}' tried to update someone else's comment ID {}", user.getUserName(), commentId);
            throw new ApiException("You are not allowed to update this comment", HttpStatus.FORBIDDEN);
        }

        comment.setText(req.getText());
        Comment updatedComment = commentRepository.save(comment);

        log.info("User '{}' updated comment ID {}", user.getUserName(), commentId);
        return modelMapper.map(updatedComment, CommentResponse.class);
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId) {
        User user = authUtil.loggedInUser();
        log.info("User '{}' attempting to delete comment ID {}", user.getUserName(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment with ID {} not found for deletion", commentId);
                    return new ApiException("Comment not found", HttpStatus.NOT_FOUND);
                });

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        boolean isOwner = comment.getAuthor().getUserId().equals(user.getUserId());

        if (isAdmin || isOwner) {
            commentRepository.delete(comment);
            log.info("Comment ID {} deleted by user '{}'", commentId, user.getUserName());
        } else {
            log.warn("User '{}' attempted unauthorized deletion of comment ID {}", user.getUserName(), commentId);
            throw new ApiException("You are not allowed to delete this comment", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public CommentPageResponse getAllComments(Integer page, Integer size, String sortBy, String sortOrder) {
        log.info("Fetching all comments (page={}, size={}, sortBy={}, order={})", page, size, sortBy, sortOrder);

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> commentPage = commentRepository.findAll(pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(c -> modelMapper.map(c, CommentResponse.class))
                .toList();

        CommentPageResponse resp = new CommentPageResponse();
        resp.setContent(comments);
        resp.setPageNumber(commentPage.getNumber());
        resp.setPageSize(commentPage.getSize());
        resp.setTotalElements(commentPage.getTotalElements());
        resp.setTotalPages(commentPage.getTotalPages());
        resp.setLastPage(commentPage.isLast());

        log.info("Fetched {} total comments", commentPage.getTotalElements());
        return resp;
    }
}
