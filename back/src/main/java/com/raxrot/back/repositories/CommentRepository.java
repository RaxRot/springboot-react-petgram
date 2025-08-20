package com.raxrot.back.repositories;

import com.raxrot.back.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByPost_Id(Long postId, Pageable pageable);
    long countByPost_Id(Long postId);
}
