package com.raxrot.back.repositories;

import com.raxrot.back.models.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    long countByFollowee_UserId(Long userId);
    long countByFollower_UserId(Long userId);

    boolean existsByFollower_UserIdAndFollowee_UserId(Long followerId, Long followeeId);
    void deleteByFollower_UserIdAndFollowee_UserId(Long followerId, Long followeeId);

    void deleteAllByFollower_UserId(Long userId);
    void deleteAllByFollowee_UserId(Long userId);
}
