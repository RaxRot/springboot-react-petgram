package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Follow;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.FollowRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.FollowService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Transactional
    @Override
    public void followUser(Long followeeId) {
        User me = authUtil.loggedInUser();
        User userToFollow = userRepository.findById(followeeId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (me.getUserId().equals(userToFollow.getUserId())) {
            throw new ApiException("You cannot follow yourself", HttpStatus.BAD_REQUEST);
        }

        if (followRepository.existsByFollower_UserIdAndFollowee_UserId(me.getUserId(), userToFollow.getUserId())) {
            return;
        }

        Follow f = new Follow();
        f.setFollower(me);
        f.setFollowee(userToFollow);
        followRepository.save(f);
    }

    @Transactional
    @Override
    public void unfollowUser(Long followeeId) {
        User me = authUtil.loggedInUser();
        User userToUnfollow = userRepository.findById(followeeId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (me.getUserId().equals(userToUnfollow.getUserId())) {
            throw new ApiException("You cannot unfollow yourself", HttpStatus.BAD_REQUEST);
        }

        followRepository.deleteByFollower_UserIdAndFollowee_UserId(me.getUserId(), userToUnfollow.getUserId());
    }

    @Override
    public long getFollowersCount(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return followRepository.countByFollowee_UserId(userId);
    }

    @Override
    public long getFollowingCount(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return followRepository.countByFollower_UserId(userId);
    }

    @Override
    public boolean isFollowing(Long targetUserId) {
        User me = authUtil.loggedInUser();
        if (me.getUserId().equals(targetUserId)) return false;
        return followRepository.existsByFollower_UserIdAndFollowee_UserId(me.getUserId(), targetUserId);
    }
}