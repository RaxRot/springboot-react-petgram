package com.raxrot.back.repositories;

import com.raxrot.back.models.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository  extends JpaRepository<Like, Long> {
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);
    long countByPost_Id(Long postId);
    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);

    void deleteAllByUser_UserId(Long userId);
}
