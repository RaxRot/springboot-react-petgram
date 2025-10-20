package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Like;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.LikeRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.LikeService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final AuthUtil authUtil;

    @Transactional
    @Override
    public void likePost(Long postId) {
        log.info("User attempting to like post ID {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while attempting to like", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        User user = authUtil.loggedInUser();

        if (likeRepository.existsByPost_IdAndUser_UserId(postId, user.getUserId())) {
            log.info("User '{}' already liked post ID {}", user.getUserName(), postId);
            return;
        }

        try {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);
            log.info("User '{}' successfully liked post ID {}", user.getUserName(), postId);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while user '{}' liked post ID {}: {}", user.getUserName(), postId, e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public void unlikePost(Long postId) {
        log.info("User attempting to unlike post ID {}", postId);

        postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while attempting to unlike", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        User user = authUtil.loggedInUser();
        likeRepository.deleteByPost_IdAndUser_UserId(postId, user.getUserId());
        log.info("User '{}' successfully unliked post ID {}", user.getUserName(), postId);
    }

    @Override
    public long getLikesCount(Long postId) {
        log.info("Fetching like count for post ID {}", postId);

        postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while fetching like count", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        long count = likeRepository.countByPost_Id(postId);
        log.info("Post ID {} has {} likes", postId, count);
        return count;
    }

    @Override
    public boolean isLikedByMe(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while checking like status", postId);
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        User user = authUtil.loggedInUser();
        boolean liked = likeRepository.existsByPost_IdAndUser_UserId(postId, user.getUserId());
        log.info("User '{}' like status for post ID {}: {}", user.getUserName(), postId, liked);
        return liked;
    }
}
