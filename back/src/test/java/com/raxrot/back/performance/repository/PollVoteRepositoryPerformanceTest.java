package com.raxrot.back.performance.repository;

import com.raxrot.back.models.*;
import com.raxrot.back.repositories.PollVoteRepository;
import com.raxrot.back.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 PollVoteRepository Performance Tests")
class PollVoteRepositoryPerformanceTest {

    @Autowired private PollVoteRepository pollVoteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 5_000;
    private static final int POLLS = 200;
    private static final int OPTIONS = 4;
    private static final int VOTES = 20_000; // must be <= USERS * POLLS to avoid duplicates

    private Long testPollId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        if (pollVoteRepository.count() > 0) return;

        // USERS
        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@t.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        // POLLS + POSTS + OPTIONS
        List<Poll> polls = new ArrayList<>(POLLS);
        for (int i = 0; i < POLLS; i++) {

            // create Post
            Post post = new Post();
            post.setUser(users.get(i % USERS));
            post.setTitle("Poll Post " + i);
            post.setContent("Poll Content " + i);
            post.setImageUrl("img/poll/" + i);
            post.setCreatedAt(LocalDateTime.now());
            post.setViewsCount(0);
            em.persist(post);

            // create Poll
            Poll poll = new Poll();
            poll.setPost(post);
            poll.setQuestion("Question " + i);

            List<PollOption> options = new ArrayList<>(OPTIONS);
            for (int o = 0; o < OPTIONS; o++) {
                PollOption option = new PollOption();
                option.setPoll(poll);
                option.setOptionText("Option " + o);
                options.add(option);
            }
            poll.setOptions(options);

            polls.add(poll);
            em.persist(poll);

            if (i % 50 == 0) {
                em.flush(); em.clear();
            }
        }
        em.flush(); em.clear();

        // VOTES: generate WITHOUT duplicates (user, poll)
        List<PollVote> votes = new ArrayList<>(VOTES);

        int u = 0; // user index
        int p = 0; // poll index

        for (int i = 0; i < VOTES; i++) {

            User user = users.get(u);
            Poll poll = polls.get(p);

            PollOption opt = poll.getOptions().get(i % OPTIONS);

            PollVote v = new PollVote();
            v.setPoll(poll);
            v.setSelectedOption(opt);
            v.setUser(user);
            votes.add(v);

            // next user; when users exhausted → next poll
            u++;
            if (u == USERS) {
                u = 0;
                p++;
                if (p == POLLS) p = 0;
            }

            if (votes.size() % 1000 == 0) {
                pollVoteRepository.saveAll(votes);
                votes.clear();
                em.flush(); em.clear();
            }
        }

        if (!votes.isEmpty()) {
            pollVoteRepository.saveAll(votes);
            em.flush(); em.clear();
        }

        testPollId = polls.get(0).getId();
        testUserId = users.get(0).getUserId();
    }

    @Test
    @DisplayName("⏱ existsByPoll_IdAndUser_UserId — performance")
    void existsByPoll_IdAndUser_UserId_performance() {
        StopWatch sw = new StopWatch("PollVoteRepository.existsByPoll_IdAndUser_UserId");

        sw.start("exists check");
        pollVoteRepository.existsByPoll_IdAndUser_UserId(testPollId, testUserId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: existsByPoll_IdAndUser_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ findByPoll_Id — performance")
    void findByPoll_Id_performance() {
        StopWatch sw = new StopWatch("PollVoteRepository.findByPoll_Id");

        sw.start("find all votes for poll");
        pollVoteRepository.findByPoll_Id(testPollId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findByPoll_Id ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}