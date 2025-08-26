package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByUser_UserName(String username, Pageable pageable);
    Page<Post> findAllByAnimalType(AnimalType animalType, Pageable pageable);
    void deleteAllByUser_UserId(Long userId);
}
