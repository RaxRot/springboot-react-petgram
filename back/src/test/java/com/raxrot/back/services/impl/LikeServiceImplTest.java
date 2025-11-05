package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Like;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.LikeRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("LikeServiceImpl Tests")
class LikeServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private AuthUtil authUtil;

    @InjectMocks
    private LikeServiceImpl likeService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");

        post = new Post();
        post.setId(100L);
        post.setTitle("Funny cat meme");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should like post successfully")
    void should_like_post_successfully() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(likeRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(false);

        likeService.likePost(100L);

        verify(likeRepository).save(any(Like.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should not save like if already liked")
    void should_not_save_if_already_liked() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(likeRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(true);

        likeService.likePost(100L);

        verify(likeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found on like")
    void should_throw_when_post_not_found_like() {
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.likePost(100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should handle DataIntegrityViolationException gracefully")
    void should_handle_data_integrity_violation() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(likeRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(false);
        willThrow(new DataIntegrityViolationException("Constraint violation"))
                .given(likeRepository).save(any(Like.class));

        // should not throw to user, just log internally
        assertThatCode(() -> likeService.likePost(100L))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should unlike post successfully")
    void should_unlike_post_successfully() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);

        likeService.unlikePost(100L);

        verify(likeRepository).deleteByPost_IdAndUser_UserId(100L, 1L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found on unlike")
    void should_throw_when_post_not_found_unlike() {
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.unlikePost(100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return correct likes count")
    void should_return_likes_count() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(likeRepository.countByPost_Id(100L)).willReturn(7L);

        long count = likeService.getLikesCount(100L);

        assertThat(count).isEqualTo(7L);
        verify(likeRepository).countByPost_Id(100L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found for getLikesCount")
    void should_throw_when_post_not_found_getLikesCount() {
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.getLikesCount(100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return true if post is liked by user")
    void should_return_true_if_liked_by_me() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(likeRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(true);

        boolean liked = likeService.isLikedByMe(100L);

        assertThat(liked).isTrue();
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return false if post is not liked by user")
    void should_return_false_if_not_liked_by_me() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(likeRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(false);

        boolean liked = likeService.isLikedByMe(100L);

        assertThat(liked).isFalse();
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found for isLikedByMe")
    void should_throw_when_post_not_found_isLikedByMe() {
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.isLikedByMe(100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }
}