package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.*;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("AnalyticsServiceImpl Tests")
class AnalyticsServiceImplTest {

    @Mock private AuthUtil authUtil;
    @Mock private PostRepository postRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private FollowRepository followRepository;
    @Mock private PetRepository petRepository;
    @Mock private DonationRepository donationRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(10L);
        user.setUserName("vlad");
    }

    @Test
    @DisplayName("Should correctly calculate all stats for logged in user")
    void should_calculate_all_user_stats_correctly() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.countByUser_UserId(10L)).willReturn(5L);
        given(likeRepository.countByUser_UserId(10L)).willReturn(15L);
        given(commentRepository.countByAuthor_UserId(10L)).willReturn(3L);
        given(postRepository.sumViewsByUser(10L)).willReturn(500L);
        given(petRepository.countByOwner_UserId(10L)).willReturn(2L);
        given(followRepository.countByFollowee_UserId(10L)).willReturn(4L);
        given(followRepository.countByFollower_UserId(10L)).willReturn(6L);

        User receiver1 = new User();
        receiver1.setUserId(10L);
        User receiver2 = new User();
        receiver2.setUserId(99L);

        Donation d1 = new Donation(1L, null, receiver1, 100L, "EUR", null);
        Donation d2 = new Donation(2L, null, receiver2, 50L, "EUR", null);
        given(donationRepository.findAll()).willReturn(List.of(d1, d2));

        // when
        UserStatsResponse stats = analyticsService.getMyStats();

        // then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalPosts()).isEqualTo(5L);
        assertThat(stats.getTotalLikes()).isEqualTo(15L);
        assertThat(stats.getTotalComments()).isEqualTo(3L);
        assertThat(stats.getTotalViews()).isEqualTo(500L);
        assertThat(stats.getTotalPets()).isEqualTo(2L);
        assertThat(stats.getTotalFollowers()).isEqualTo(4L);
        assertThat(stats.getTotalFollowing()).isEqualTo(6L);
        assertThat(stats.getTotalDonationsReceived()).isEqualTo(100L);

        // verify that each repository was called exactly once
        verify(postRepository).countByUser_UserId(10L);
        verify(likeRepository).countByUser_UserId(10L);
        verify(commentRepository).countByAuthor_UserId(10L);
        verify(postRepository).sumViewsByUser(10L);
        verify(petRepository).countByOwner_UserId(10L);
        verify(followRepository).countByFollowee_UserId(10L);
        verify(followRepository).countByFollower_UserId(10L);
        verify(donationRepository).findAll();
    }

    @Test
    @DisplayName("Should return zero donations when none received")
    void should_return_zero_when_no_donations_for_user() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.countByUser_UserId(10L)).willReturn(0L);
        given(likeRepository.countByUser_UserId(10L)).willReturn(0L);
        given(commentRepository.countByAuthor_UserId(10L)).willReturn(0L);
        given(postRepository.sumViewsByUser(10L)).willReturn(0L);
        given(petRepository.countByOwner_UserId(10L)).willReturn(0L);
        given(followRepository.countByFollowee_UserId(10L)).willReturn(0L);
        given(followRepository.countByFollower_UserId(10L)).willReturn(0L);
        given(donationRepository.findAll()).willReturn(List.of());

        // when
        UserStatsResponse stats = analyticsService.getMyStats();

        // then
        assertThat(stats.getTotalDonationsReceived()).isZero();
        assertThat(stats.getTotalPosts()).isZero();
    }
}