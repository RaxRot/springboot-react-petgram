package com.raxrot.back.repositories;

import com.raxrot.back.models.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);
    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);
    Page<Bookmark> findAllByUser_UserId(Long userId, Pageable p);
}
