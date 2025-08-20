package com.raxrot.back.services;

public interface LikeService {
    void likePost(Long postId);
    void unlikePost(Long postId);
    long getLikesCount(Long postId);
    boolean isLikedByMe(Long postId);
}
