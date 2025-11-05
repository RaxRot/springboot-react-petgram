package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Follow;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.FollowRepository;
import com.raxrot.back.repositories.UserRepository;
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
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("FollowServiceImpl Tests")
class FollowServiceImplTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthUtil authUtil;

    @InjectMocks
    private FollowServiceImpl followService;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = new User();
        me.setUserId(1L);
        me.setUserName("vlad");

        other = new User();
        other.setUserId(2L);
        other.setUserName("dasha");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should follow another user successfully")
    void should_follow_user_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(followRepository.existsByFollower_UserIdAndFollowee_UserId(1L, 2L)).willReturn(false);

        followService.followUser(2L);

        verify(followRepository).save(any(Follow.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user tries to follow themselves")
    void should_throw_when_following_self() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        assertThatThrownBy(() -> followService.followUser(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot follow yourself")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when target user not found on follow")
    void should_throw_when_follow_target_not_found() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.followUser(2L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should not save follow if already following")
    void should_not_save_when_already_following() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(followRepository.existsByFollower_UserIdAndFollowee_UserId(1L, 2L)).willReturn(true);

        followService.followUser(2L);

        verify(followRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should unfollow user successfully")
    void should_unfollow_user_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));

        followService.unfollowUser(2L);

        verify(followRepository).deleteByFollower_UserIdAndFollowee_UserId(1L, 2L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when unfollowing self")
    void should_throw_when_unfollowing_self() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        assertThatThrownBy(() -> followService.unfollowUser(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot unfollow yourself");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when target user not found on unfollow")
    void should_throw_when_target_not_found_unfollow() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollowUser(2L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return followers count successfully")
    void should_return_followers_count() {
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(followRepository.countByFollowee_UserId(2L)).willReturn(5L);

        long count = followService.getFollowersCount(2L);

        assertThat(count).isEqualTo(5L);
        verify(followRepository).countByFollowee_UserId(2L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return following count successfully")
    void should_return_following_count() {
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(followRepository.countByFollower_UserId(1L)).willReturn(3L);

        long count = followService.getFollowingCount(1L);

        assertThat(count).isEqualTo(3L);
        verify(followRepository).countByFollower_UserId(1L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user not found for count methods")
    void should_throw_when_user_not_found_for_counts() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.getFollowersCount(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");

        assertThatThrownBy(() -> followService.getFollowingCount(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return true if following target user")
    void should_return_true_if_following() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(followRepository.existsByFollower_UserIdAndFollowee_UserId(1L, 2L)).willReturn(true);

        boolean result = followService.isFollowing(2L);

        assertThat(result).isTrue();
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return false if not following target user")
    void should_return_false_if_not_following() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(followRepository.existsByFollower_UserIdAndFollowee_UserId(1L, 2L)).willReturn(false);

        boolean result = followService.isFollowing(2L);

        assertThat(result).isFalse();
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return false when checking follow status for self")
    void should_return_false_when_checking_self_follow() {
        given(authUtil.loggedInUser()).willReturn(me);

        boolean result = followService.isFollowing(1L);

        assertThat(result).isFalse();
        verify(followRepository, never()).existsByFollower_UserIdAndFollowee_UserId(anyLong(), anyLong());
    }
}