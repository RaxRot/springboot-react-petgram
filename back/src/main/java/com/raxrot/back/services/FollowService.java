package com.raxrot.back.services;

public interface FollowService {
   void followUser(Long followeeId) ;
   void unfollowUser(Long followeeId);
   long getFollowersCount(Long userId);
   long getFollowingCount(Long userId);
}
