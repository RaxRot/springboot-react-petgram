package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("CommentServiceImpl Tests")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private AuthUtil authUtil;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private User admin;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");
        user.setBanned(false);
        user.setRoles(Set.of());

        admin = new User();
        admin.setUserId(2L);
        admin.setUserName("admin");
        admin.setRoles(Set.of(new Role(1L, AppRole.ROLE_ADMIN)));

        post = new Post();
        post.setId(100L);
        post.setTitle("Test Post");

        comment = new Comment();
        comment.setId(10L);
        comment.setText("Nice post!");
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should add comment successfully")
    void should_add_comment_successfully() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(modelMapper.map(any(CommentRequest.class), eq(Comment.class))).willReturn(comment);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(modelMapper.map(any(Comment.class), eq(CommentResponse.class)))
                .willReturn(new CommentResponse(10L, "Nice post!", null, null, null));

        CommentRequest req = new CommentRequest("Nice post!");
        CommentResponse resp = commentService.addComment(100L, req);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getText()).isEqualTo("Nice post!");
        verify(commentRepository).save(any(Comment.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when banned user tries to comment")
    void should_throw_when_banned_user_adds_comment() {
        user.setBanned(true);
        given(authUtil.loggedInUser()).willReturn(user);

        CommentRequest req = new CommentRequest("test");
        assertThatThrownBy(() -> commentService.addComment(100L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User is banned");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found on addComment")
    void should_throw_when_post_not_found_addComment() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        CommentRequest req = new CommentRequest("hello");
        assertThatThrownBy(() -> commentService.addComment(100L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should fetch comments successfully")
    void should_fetch_comments_successfully() {
        given(postRepository.existsById(100L)).willReturn(true);
        Page<Comment> page = new PageImpl<>(List.of(comment));
        given(commentRepository.findAllByPost_Id(eq(100L), any(Pageable.class))).willReturn(page);
        given(modelMapper.map(any(Comment.class), eq(CommentResponse.class)))
                .willReturn(new CommentResponse(10L, "Nice post!", null, null, null));

        CommentPageResponse resp = commentService.getComments(100L, 0, 5, "id", "asc");

        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getText()).isEqualTo("Nice post!");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when fetching comments for non-existing post")
    void should_throw_when_post_not_found_getComments() {
        given(postRepository.existsById(100L)).willReturn(false);
        assertThatThrownBy(() -> commentService.getComments(100L, 0, 5, "id", "desc"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should update comment successfully by author")
    void should_update_comment_successfully() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(modelMapper.map(any(Comment.class), eq(CommentResponse.class)))
                .willReturn(new CommentResponse(10L, "Updated text", null, null, null));

        CommentRequest req = new CommentRequest("Updated text");
        CommentResponse resp = commentService.updateComment(10L, req);

        assertThat(resp.getText()).isEqualTo("Updated text");
        verify(commentRepository).save(any(Comment.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user tries to update someone else's comment")
    void should_throw_when_updating_not_own_comment() {
        User other = new User();
        other.setUserId(99L);
        comment.setAuthor(other);

        given(authUtil.loggedInUser()).willReturn(user);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        CommentRequest req = new CommentRequest("Hack edit");
        assertThatThrownBy(() -> commentService.updateComment(10L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when banned user tries to update comment")
    void should_throw_when_banned_user_updates_comment() {
        user.setBanned(true);
        given(authUtil.loggedInUser()).willReturn(user);

        CommentRequest req = new CommentRequest("some text");
        assertThatThrownBy(() -> commentService.updateComment(10L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User is banned");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should delete comment successfully by owner")
    void should_delete_comment_by_owner() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        commentService.deleteComment(10L);

        verify(commentRepository).delete(comment);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should delete comment successfully by admin")
    void should_delete_comment_by_admin() {
        given(authUtil.loggedInUser()).willReturn(admin);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        commentService.deleteComment(10L);

        verify(commentRepository).delete(comment);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when deleting comment without permission")
    void should_throw_when_deleting_without_permission() {
        User other = new User();
        other.setUserId(99L);
        comment.setAuthor(other);

        given(authUtil.loggedInUser()).willReturn(user);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed");
        verify(commentRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should fetch all comments paginated")
    void should_fetch_all_comments_paginated() {
        Page<Comment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 5), 1);
        given(commentRepository.findAll(any(Pageable.class))).willReturn(page);
        given(modelMapper.map(any(Comment.class), eq(CommentResponse.class)))
                .willReturn(new CommentResponse(10L, "text", null, null, null));

        CommentPageResponse resp = commentService.getAllComments(0, 5, "id", "desc");

        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }
}