package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.*;
import com.raxrot.back.services.AnalyticsService;
import com.raxrot.back.utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AuthUtil authUtil;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final PetRepository petRepository;
    private final DonationRepository donationRepository;

    @Override
    public UserStatsResponse getMyStats() {
        User me = authUtil.loggedInUser();
        log.info("Calculating statistics for user: {}", me.getUserName());

        long totalPosts = postRepository.countByUser_UserId(me.getUserId());
        long totalLikes = likeRepository.countByUser_UserId(me.getUserId());
        long totalComments = commentRepository.countByAuthor_UserId(me.getUserId());
        long totalViews = postRepository.sumViewsByUser(me.getUserId());
        long totalPets = petRepository.countByOwner_UserId(me.getUserId());
        long totalFollowers = followRepository.countByFollowee_UserId(me.getUserId());
        long totalFollowing = followRepository.countByFollower_UserId(me.getUserId());
        long totalDonationsReceived = donationRepository.findAll().stream()
                .filter(d -> d.getReceiver().getUserId().equals(me.getUserId()))
                .mapToLong(Donation::getAmount)
                .sum();

        log.info("User '{}' stats calculated: posts={}, likes={}, comments={}, views={}, pets={}, followers={}, following={}, donations={}",
                me.getUserName(), totalPosts, totalLikes, totalComments, totalViews, totalPets, totalFollowers, totalFollowing, totalDonationsReceived);

        return new UserStatsResponse(
                totalPosts,
                totalLikes,
                totalComments,
                totalViews,
                totalPets,
                totalFollowers,
                totalFollowing,
                totalDonationsReceived
        );
    }
}
