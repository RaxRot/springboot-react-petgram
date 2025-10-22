package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Like;
import com.raxrot.back.models.Post;
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
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User alice;
    private User bob;
    private User charlie;
    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        // Users
        alice = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        bob = userRepository.save(new User("bob", "bob@example.com", "pwd"));
        charlie = userRepository.save(new User("charlie", "charlie@example.com", "pwd"));

        // Posts
        post1 = postRepository.save(new Post(null, "Post 1", "Content 1", null, AnimalType.DOG, null, null, alice, List.of(), List.of(), List.of(), 0));
        post2 = postRepository.save(new Post(null, "Post 2", "Content 2", null, AnimalType.CAT, null, null, bob, List.of(), List.of(), List.of(), 0));

        // Likes
        likeRepository.save(new Like(null, post1, alice, null));
        likeRepository.save(new Like(null, post1, bob, null));
        likeRepository.save(new Like(null, post2, alice, null));
    }

    @Test
    @DisplayName("existsByPost_IdAndUser_UserId should return true when a user has liked a post")
    void existsByPostIdAndUserId_ReturnsTrueWhenLiked() {
        boolean exists = likeRepository.existsByPost_IdAndUser_UserId(post1.getId(), alice.getUserId());
        assertThat(exists).isTrue();

        boolean notExists = likeRepository.existsByPost_IdAndUser_UserId(post2.getId(), charlie.getUserId());
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("countByPost_Id should return number of likes on a given post")
    void countByPostId_ReturnsLikeCount() {
        long countPost1 = likeRepository.countByPost_Id(post1.getId());
        long countPost2 = likeRepository.countByPost_Id(post2.getId());

        assertThat(countPost1).isEqualTo(2);
        assertThat(countPost2).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteByPost_IdAndUser_UserId should remove a specific like")
    void deleteByPostIdAndUserId_RemovesLike() {
        likeRepository.deleteByPost_IdAndUser_UserId(post1.getId(), alice.getUserId());

        boolean stillExists = likeRepository.existsByPost_IdAndUser_UserId(post1.getId(), alice.getUserId());
        assertThat(stillExists).isFalse();

        long remainingLikes = likeRepository.countByPost_Id(post1.getId());
        assertThat(remainingLikes).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteAllByUser_UserId should remove all likes from a user")
    void deleteAllByUserId_RemovesUserLikes() {
        likeRepository.deleteAllByUser_UserId(alice.getUserId());

        long countAfterDelete = likeRepository.countByUser_UserId(alice.getUserId());
        assertThat(countAfterDelete).isZero();

        // Ensure Bob’s like still exists
        assertThat(likeRepository.existsByPost_IdAndUser_UserId(post1.getId(), bob.getUserId())).isTrue();
    }

    @Test
    @DisplayName("countByUser_UserId should return total number of likes given by a user")
    void countByUserId_ReturnsTotalLikesGivenByUser() {
        long aliceLikes = likeRepository.countByUser_UserId(alice.getUserId());
        long bobLikes = likeRepository.countByUser_UserId(bob.getUserId());
        long charlieLikes = likeRepository.countByUser_UserId(charlie.getUserId());

        assertThat(aliceLikes).isEqualTo(2);
        assertThat(bobLikes).isEqualTo(1);
        assertThat(charlieLikes).isZero();
    }
}
