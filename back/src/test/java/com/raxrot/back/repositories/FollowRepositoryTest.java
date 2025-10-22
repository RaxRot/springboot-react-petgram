package com.raxrot.back.repositories;

import com.raxrot.back.models.Follow;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        bob = userRepository.save(new User("bob", "bob@example.com", "pwd"));
        charlie = userRepository.save(new User("charlie", "charlie@example.com", "pwd"));

        // Alice follows Bob
        followRepository.save(new Follow(null, alice, bob, null));
        // Bob follows Alice
        followRepository.save(new Follow(null, bob, alice, null));
        // Charlie follows Alice
        followRepository.save(new Follow(null, charlie, alice, null));
    }

    @Test
    @DisplayName("countByFollowee_UserId should return correct number of followers")
    void countByFollowee_UserId_ReturnsCorrectCount() {
        long aliceFollowers = followRepository.countByFollowee_UserId(alice.getUserId());
        long bobFollowers = followRepository.countByFollowee_UserId(bob.getUserId());

        assertThat(aliceFollowers).isEqualTo(2); // Bob and Charlie follow Alice
        assertThat(bobFollowers).isEqualTo(1);   // Alice follows Bob
    }

    @Test
    @DisplayName("countByFollower_UserId should return correct number of followings")
    void countByFollower_UserId_ReturnsCorrectCount() {
        long aliceFollowing = followRepository.countByFollower_UserId(alice.getUserId());
        long bobFollowing = followRepository.countByFollower_UserId(bob.getUserId());

        assertThat(aliceFollowing).isEqualTo(1); // Alice follows Bob
        assertThat(bobFollowing).isEqualTo(1);   // Bob follows Alice
    }

    @Test
    @DisplayName("existsByFollower_UserIdAndFollowee_UserId should detect existing follow")
    void existsByFollowerAndFollowee_ReturnsTrueIfExists() {
        boolean exists = followRepository.existsByFollower_UserIdAndFollowee_UserId(alice.getUserId(), bob.getUserId());
        boolean notExists = followRepository.existsByFollower_UserIdAndFollowee_UserId(bob.getUserId(), charlie.getUserId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("deleteByFollower_UserIdAndFollowee_UserId should remove specific follow")
    void deleteByFollowerAndFollowee_RemovesFollow() {
        followRepository.deleteByFollower_UserIdAndFollowee_UserId(alice.getUserId(), bob.getUserId());

        boolean exists = followRepository.existsByFollower_UserIdAndFollowee_UserId(alice.getUserId(), bob.getUserId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("deleteAllByFollower_UserId should remove all followings of a user")
    void deleteAllByFollower_RemovesAllFollowings() {
        followRepository.deleteAllByFollower_UserId(alice.getUserId());

        long count = followRepository.countByFollower_UserId(alice.getUserId());
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("deleteAllByFollowee_UserId should remove all followers of a user")
    void deleteAllByFollowee_RemovesAllFollowers() {
        followRepository.deleteAllByFollowee_UserId(alice.getUserId());

        long count = followRepository.countByFollowee_UserId(alice.getUserId());
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("saving duplicate follower-followee should fail due to unique constraint")
    void savingDuplicateFollow_ShouldFail() {
        Follow duplicate = new Follow(null, alice, bob, null);

        try {
            followRepository.saveAndFlush(duplicate);
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("constraint"); // DB constraint violation
        }
    }
}
