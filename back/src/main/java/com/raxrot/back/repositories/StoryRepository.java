package com.raxrot.back.repositories;

import com.raxrot.back.models.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface StoryRepository extends JpaRepository<Story, Long> {

    Page<Story> findAllByUser_UserIdAndExpiresAtAfter(Long userId, LocalDateTime now, Pageable p);

    @Query("""
        select s from Story s
        where s.expiresAt > :now and s.user.userId in (
           select f.followee.userId from Follow f where f.follower.userId = :meId
        )
        order by s.createdAt desc, s.id desc
    """)
    Page<Story> findFollowingStories(@Param("meId") Long meId,
                                     @Param("now") LocalDateTime now,
                                     Pageable p);

    long deleteByExpiresAtBefore(LocalDateTime time);
}

