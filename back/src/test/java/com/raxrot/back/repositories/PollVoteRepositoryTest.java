package com.raxrot.back.repositories;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PollVoteRepositoryTest {

    @Autowired
    private PollVoteRepository pollVoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    private User user1;
    private User user2;
    private Post post;
    private Poll poll;
    private PollOption option1;
    private PollOption option2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "pwd"));

        post = postRepository.save(new Post(null, "Poll Post", "desc", "img", AnimalType.DOG, null, null, user1,
                List.of(), List.of(), List.of(), 0));

        poll = new Poll();
        poll.setPost(post);
        poll.setQuestion("What is your favorite color?");
        poll = pollRepository.save(poll);

        option1 = new PollOption(null, poll, "Red", 0L);
        option2 = new PollOption(null, poll, "Blue", 0L);
        pollOptionRepository.saveAll(List.of(option1, option2));
    }

    @Test
    @DisplayName("should verify if a user has already voted in a poll")
    void existsByPoll_IdAndUser_UserId_returnsTrueIfUserVoted() {
        // arrange
        pollVoteRepository.save(new PollVote(null, poll, user1, option1, null));

        // act
        boolean exists = pollVoteRepository.existsByPoll_IdAndUser_UserId(poll.getId(), user1.getUserId());
        boolean notExists = pollVoteRepository.existsByPoll_IdAndUser_UserId(poll.getId(), user2.getUserId());

        // assert
        assertTrue(exists, "Should return true for user who voted");
        assertFalse(notExists, "Should return false for user who did not vote");
    }

    @Test
    @DisplayName("should find all votes by poll id")
    void findByPoll_Id_returnsAllVotesForPoll() {
        // arrange
        pollVoteRepository.save(new PollVote(null, poll, user1, option1, null));
        pollVoteRepository.save(new PollVote(null, poll, user2, option2, null));

        // act
        List<PollVote> votes = (List<PollVote>) pollVoteRepository.findByPoll_Id(poll.getId());

        // assert
        assertEquals(2, votes.size(), "Should return all votes for given poll");
        assertEquals(List.of(user1.getUserId(), user2.getUserId()),
                votes.stream().map(v -> v.getUser().getUserId()).toList());
    }

    @Test
    @DisplayName("should return empty when poll has no votes")
    void findByPoll_Id_returnsEmptyListWhenNoVotes() {
        // act
        List<PollVote> votes = (List<PollVote>) pollVoteRepository.findByPoll_Id(poll.getId());

        // assert
        assertTrue(votes.isEmpty(), "Should return empty list when poll has no votes");
    }
}
