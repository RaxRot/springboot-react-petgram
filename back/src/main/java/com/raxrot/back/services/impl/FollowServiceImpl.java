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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Transactional
    @Override
    public void followUser(Long followeeId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to follow user with ID {}", me.getUserName(), followeeId);

        User userToFollow = userRepository.findById(followeeId)
                .orElseThrow(() -> {
                    log.error("User '{}' tried to follow non-existing user ID {}", me.getUserName(), followeeId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        if (me.getUserId().equals(userToFollow.getUserId())) {
            log.warn("User '{}' attempted to follow themselves", me.getUserName());
            throw new ApiException("You cannot follow yourself", HttpStatus.BAD_REQUEST);
        }

        if (followRepository.existsByFollower_UserIdAndFollowee_UserId(me.getUserId(), userToFollow.getUserId())) {
            log.info("User '{}' already follows user ID {}", me.getUserName(), followeeId);
            return;
        }

        Follow f = new Follow();
        f.setFollower(me);
        f.setFollowee(userToFollow);
        followRepository.save(f);

        log.info("User '{}' successfully followed user '{}'", me.getUserName(), userToFollow.getUserName());
    }

    @Transactional
    @Override
    public void unfollowUser(Long followeeId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to unfollow user with ID {}", me.getUserName(), followeeId);

        User userToUnfollow = userRepository.findById(followeeId)
                .orElseThrow(() -> {
                    log.error("User '{}' tried to unfollow non-existing user ID {}", me.getUserName(), followeeId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        if (me.getUserId().equals(userToUnfollow.getUserId())) {
            log.warn("User '{}' attempted to unfollow themselves", me.getUserName());
            throw new ApiException("You cannot unfollow yourself", HttpStatus.BAD_REQUEST);
        }

        followRepository.deleteByFollower_UserIdAndFollowee_UserId(me.getUserId(), userToUnfollow.getUserId());
        log.info("User '{}' successfully unfollowed user '{}'", me.getUserName(), userToUnfollow.getUserName());
    }

    @Override
    public long getFollowersCount(Long userId) {
        log.info("Fetching followers count for user ID {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Attempted to fetch followers for non-existing user ID {}", userId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        long count = followRepository.countByFollowee_UserId(userId);
        log.info("User ID {} has {} followers", userId, count);
        return count;
    }

    @Override
    public long getFollowingCount(Long userId) {
        log.info("Fetching following count for user ID {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Attempted to fetch following list for non-existing user ID {}", userId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        long count = followRepository.countByFollower_UserId(userId);
        log.info("User ID {} follows {} users", userId, count);
        return count;
    }

    @Override
    public boolean isFollowing(Long targetUserId) {
        User me = authUtil.loggedInUser();
        if (me.getUserId().equals(targetUserId)) {
            log.debug("User '{}' checked follow status for themselves — returning false", me.getUserName());
            return false;
        }

        boolean following = followRepository.existsByFollower_UserIdAndFollowee_UserId(me.getUserId(), targetUserId);
        log.info("User '{}' following status for user ID {}: {}", me.getUserName(), targetUserId, following);
        return following;
    }
}
