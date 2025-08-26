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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final AuthUtil authUtil;


    @Transactional
    @Override
    public void likePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));

        User user = authUtil.loggedInUser();

        if (likeRepository.existsByPost_IdAndUser_UserId(postId, user.getUserId())) {
            return;
        }

        try {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);

        } catch (DataIntegrityViolationException e) {

        }
    }

    @Transactional
    @Override
    public void unlikePost(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));

        User user = authUtil.loggedInUser();
        likeRepository.deleteByPost_IdAndUser_UserId(postId, user.getUserId());
    }

    @Override
    public long getLikesCount(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));
        return likeRepository.countByPost_Id(postId);
    }

    @Override
    public boolean isLikedByMe(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));
        User user = authUtil.loggedInUser();
        return likeRepository.existsByPost_IdAndUser_UserId(postId, user.getUserId());
    }
}