package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Follow;
import com.raxrot.back.models.Post;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

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
    @DisplayName("should find all posts by username")
    void findAllByUser_UserName_returnsUserPosts() {
        // arrange
        Post p1 = new Post(null, "Alice 1", "text", "img1", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 10);
        Post p2 = new Post(null, "Alice 2", "text", "img2", AnimalType.CAT, null, null, user1, List.of(), List.of(), List.of(), 5);
        Post p3 = new Post(null, "Bob", "text", "img3", AnimalType.BIRD, null, null, user2, List.of(), List.of(), List.of(), 2);
        postRepository.saveAll(List.of(p1, p2, p3));

        Pageable pageable = PageRequest.of(0, 10);

        // act
        Page<Post> result = postRepository.findAllByUser_UserName("alice", pageable);

        // assert
        assertEquals(2, result.getTotalElements());
        List<String> titles = result.map(Post::getTitle).toList();
        assertEquals(List.of("Alice 1", "Alice 2"), titles);
    }

    @Test
    @DisplayName("should find all posts by animal type")
    void findAllByAnimalType_returnsValidPosts() {
        // arrange
        Post p1 = new Post(null, "Dog Post", "woof", "dogimg", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 0);
        Post p2 = new Post(null, "Cat Post", "meow", "catimg", AnimalType.CAT, null, null, user2, List.of(), List.of(), List.of(), 0);
        Post p3 = new Post(null, "Another Dog", "bark", "dogimg2", AnimalType.DOG, null, null, user2, List.of(), List.of(), List.of(), 0);
        postRepository.saveAll(List.of(p1, p2, p3));

        Pageable pageable = PageRequest.of(0, 10);

        // act
        Page<Post> result = postRepository.findAllByAnimalType(AnimalType.DOG, pageable);

        // assert
        assertEquals(2, result.getTotalElements(), "Should return only DOG posts");
        List<String> titles = result.map(Post::getTitle).toList();
        assertEquals(List.of("Dog Post", "Another Dog"), titles);
    }

    @Test
    @DisplayName("should delete all posts by user id")
    void deleteAllByUser_UserId_deletesPosts() {
        // arrange
        postRepository.save(new Post(null, "p1", "c", "img", AnimalType.OTHER, null, null, user1, List.of(), List.of(), List.of(), 0));
        postRepository.save(new Post(null, "p2", "c", "img", AnimalType.CAT, null, null, user1, List.of(), List.of(), List.of(), 0));
        postRepository.save(new Post(null, "p3", "c", "img", AnimalType.DOG, null, null, user2, List.of(), List.of(), List.of(), 0));

        // act
        postRepository.deleteAllByUser_UserId(user1.getUserId());

        // assert
        assertEquals(1, postRepository.count(), "Only Bob's post should remain");
    }

    @Test
    @DisplayName("should find following feed posts ordered by creation")
    void findFollowingFeed_returnsFollowedUsersPosts() {
        // arrange
        followRepository.save(new Follow(null, follower, user1, null));
        followRepository.save(new Follow(null, follower, user2, null));

        Post p1 = new Post(null, "Post A1", "content", "url1", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 0);
        Post p2 = new Post(null, "Post B1", "content", "url2", AnimalType.CAT, null, null, user2, List.of(), List.of(), List.of(), 0);
        postRepository.saveAll(List.of(p1, p2));

        Pageable pageable = PageRequest.of(0, 10);

        // act
        Page<Post> result = postRepository.findFollowingFeed(follower.getUserId(), pageable);

        // assert
        assertEquals(2, result.getTotalElements(), "Should return posts from followed users");
        List<String> titles = result.map(Post::getTitle).toList();
        assertEquals(List.of("Post A1", "Post B1"), titles);
    }

    @Test
    @DisplayName("should count posts by user id")
    void countByUser_UserId_returnsCorrectCount() {
        // arrange
        postRepository.save(new Post(null, "p1", "c", "img", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 0));
        postRepository.save(new Post(null, "p2", "c", "img", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 0));
        postRepository.save(new Post(null, "p3", "c", "img", AnimalType.DOG, null, null, user2, List.of(), List.of(), List.of(), 0));

        // act
        long count = postRepository.countByUser_UserId(user1.getUserId());

        // assert
        assertEquals(2, count);
    }

    @Test
    @DisplayName("should sum views count by user id")
    void sumViewsByUser_returnsSumOfViews() {
        // arrange
        postRepository.save(new Post(null, "p1", "c", "img", AnimalType.DOG, null, null, user1, List.of(), List.of(), List.of(), 100));
        postRepository.save(new Post(null, "p2", "c", "img", AnimalType.CAT, null, null, user1, List.of(), List.of(), List.of(), 50));
        postRepository.save(new Post(null, "p3", "c", "img", AnimalType.DOG, null, null, user2, List.of(), List.of(), List.of(), 25));

        // act
        long totalViews = postRepository.sumViewsByUser(user1.getUserId());

        // assert
        assertEquals(150, totalViews, "Should sum all views of user's posts");
    }
}
