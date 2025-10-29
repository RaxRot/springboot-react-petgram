package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
//test
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByUser_UserName(String username, Pageable pageable);
    Page<Post> findAllByAnimalType(AnimalType animalType, Pageable pageable);
    void deleteAllByUser_UserId(Long userId);

    @Query("""
  select p from Post p
  where p.user.userId in (
    select f.followee.userId from Follow f
    where f.follower.userId = :meId
  )
""")
    Page<Post> findFollowingFeed(@Param("meId") Long meId, Pageable pageable);

    long countByUser_UserId(Long userId);

    @Query("SELECT COALESCE(SUM(p.viewsCount), 0) FROM Post p WHERE p.user.userId = :userId")
    long sumViewsByUser(@Param("userId") Long userId);

    Post findTopByCreatedAtBetweenOrderByLikesDesc(LocalDateTime start, LocalDateTime end);

    Post findTopByOrderByViewsCountDesc();
}
