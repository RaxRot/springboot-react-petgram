package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Poll;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PollRepositoryTest {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        post1 = postRepository.save(new Post(null, "Post 1", "desc1", "img1",
                AnimalType.DOG, null, null, user, null, null, null, 0));
        post2 = postRepository.save(new Post(null, "Post 2", "desc2", "img2",
                AnimalType.CAT, null, null, user, null, null, null, 0));
    }

    @Test
    @DisplayName("should find poll by associated post id")
    void findByPost_Id_returnsCorrectPoll() {
        // arrange
        Poll poll = new Poll();
        poll.setPost(post1);
        poll.setQuestion("Do you like dogs?");
        pollRepository.save(poll);

        // act
        Optional<Poll> foundPoll = pollRepository.findByPost_Id(post1.getId());

        // assert
        assertTrue(foundPoll.isPresent(), "Poll should be found by post id");
        assertEquals("Do you like dogs?", foundPoll.get().getQuestion());
        assertEquals(post1.getId(), foundPoll.get().getPost().getId());
    }

    @Test
    @DisplayName("should return empty when no poll exists for given post id")
    void findByPost_Id_returnsEmptyWhenNoPollExists() {
        // act
        Optional<Poll> result = pollRepository.findByPost_Id(post2.getId());

        // assert
        assertTrue(result.isEmpty(), "Should return empty Optional when no poll exists");
    }

    @Test
    @DisplayName("should handle multiple polls for different posts independently")
    void findByPost_Id_handlesMultiplePollsCorrectly() {
        // arrange
        Poll poll1 = new Poll();
        poll1.setPost(post1);
        poll1.setQuestion("Favorite dog breed?");
        pollRepository.save(poll1);

        Poll poll2 = new Poll();
        poll2.setPost(post2);
        poll2.setQuestion("Favorite cat color?");
        pollRepository.save(poll2);

        // act
        Optional<Poll> found1 = pollRepository.findByPost_Id(post1.getId());
        Optional<Poll> found2 = pollRepository.findByPost_Id(post2.getId());

        // assert
        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertEquals("Favorite dog breed?", found1.get().getQuestion());
        assertEquals("Favorite cat color?", found2.get().getQuestion());
    }
}
