package com.raxrot.back.repositories;

import com.raxrot.back.models.Follow;
import com.raxrot.back.models.Story;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class StoryRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    private User user1;
    private User user2;
    private User follower;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "pwd"));
        follower = userRepository.save(new User("charlie", "charlie@example.com", "pwd"));
    }

    @Test
    @DisplayName("should find all stories by userId where expiresAt is after given time")
    void findAllByUser_UserIdAndExpiresAtAfter_returnsValidStories() {
        // arrange
        LocalDateTime now = LocalDateTime.now();

        Story activeStory = new Story(null, user1, "url1", now.minusHours(1), now.plusHours(2), 0L);
        Story expiredStory = new Story(null, user1, "url2", now.minusHours(3), now.minusMinutes(10), 0L);
        storyRepository.saveAll(List.of(activeStory, expiredStory));

        Pageable pageable = PageRequest.of(0, 10);

        // act
        Page<Story> result = storyRepository.findAllByUser_UserIdAndExpiresAtAfter(user1.getUserId(), now, pageable);

        // assert
        assertEquals(1, result.getTotalElements(), "Only one active story should be returned");
        assertEquals("url1", result.getContent().get(0).getImageUrl());
    }

    @Test
    @DisplayName("should return following users' active stories ordered by creation date desc")
    void findFollowingStories_returnsStoriesOfFollowedUsers() {
        // arrange
        LocalDateTime now = LocalDateTime.now();

        // follower (Charlie) follows Alice and Bob
        followRepository.save(new Follow(null, follower, user1, now));
        followRepository.save(new Follow(null, follower, user2, now));

        Story s1 = new Story(null, user1, "story1", now.minusHours(1), now.plusHours(3), 0L);
        Story s2 = new Story(null, user2, "story2", now.minusMinutes(30), now.plusHours(2), 0L);
        Story expired = new Story(null, user1, "oldStory", now.minusDays(1), now.minusMinutes(5), 0L);
        storyRepository.saveAll(List.of(s1, s2, expired));

        Pageable pageable = PageRequest.of(0, 10);

        // act
        Page<Story> result = storyRepository.findFollowingStories(follower.getUserId(), now, pageable);

        // assert
        assertEquals(2, result.getTotalElements(), "Should return only active stories from followed users");

        // Проверяем порядок по imageUrl, потому что createdAt у всех одинаковый (Hibernate ставит текущее время)
        List<String> imageUrls = result.getContent().stream().map(Story::getImageUrl).toList();
        assertEquals(List.of("story2", "story1"), imageUrls,
                "Stories should be ordered logically by creation order (most recent first)");
    }

    @Test
    @DisplayName("should delete stories that expired before given time")
    void deleteByExpiresAtBefore_deletesExpiredStories() {
        // arrange
        LocalDateTime now = LocalDateTime.now();

        Story expired1 = new Story(null, user1, "old1", now.minusDays(2), now.minusDays(1), 0L);
        Story expired2 = new Story(null, user2, "old2", now.minusHours(5), now.minusHours(2), 0L);
        Story active = new Story(null, user1, "new", now.minusHours(1), now.plusHours(2), 0L);

        storyRepository.saveAll(List.of(expired1, expired2, active));

        // act
        long deletedCount = storyRepository.deleteByExpiresAtBefore(now);

        // assert
        assertEquals(2, deletedCount, "Should delete only expired stories");
        assertEquals(1, storyRepository.count(), "Only one story should remain");
    }
}
