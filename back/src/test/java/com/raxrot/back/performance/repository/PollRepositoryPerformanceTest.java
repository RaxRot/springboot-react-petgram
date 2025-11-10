package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Poll;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PollRepository;
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
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 PollRepository Performance Tests")
class PollRepositoryPerformanceTest {

    @Autowired private PollRepository pollRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int POLLS = 15_000;
    private Long existingPostId;
    private Long nonExistingPostId = 999_999L;

    @BeforeEach
    void setUp() {
        if (pollRepository.count() > 0) return;


        User user = new User("perfUser", "perf@t.com", "pwd");
        userRepository.save(user);
        em.flush(); em.clear();


        List<Poll> pollList = new ArrayList<>(POLLS);

        for (int i = 0; i < POLLS; i++) {
            Post post = new Post();
            post.setUser(user);
            post.setTitle("P" + i);
            post.setContent("C" + i);
            post.setImageUrl("img/" + i);
            post.setCreatedAt(LocalDateTime.now());
            post.setViewsCount(0);
            em.persist(post);

            Poll poll = new Poll();
            poll.setPost(post);
            poll.setQuestion("Question " + i);

            pollList.add(poll);
            em.persist(poll);

            if (i % 500 == 0) {
                em.flush(); em.clear();
            }
        }

        em.flush(); em.clear();

        existingPostId = 1L;
    }

    @Test
    @DisplayName("⏱ findByPost_Id — performance")
    void findByPost_Id_performance() {
        StopWatch sw = new StopWatch("PollRepository.findByPost_Id");

        sw.start("find existing");
        Optional<Poll> p1 = pollRepository.findByPost_Id(existingPostId);
        sw.stop();

        sw.start("find non-existing");
        pollRepository.findByPost_Id(nonExistingPostId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findByPost_Id ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}