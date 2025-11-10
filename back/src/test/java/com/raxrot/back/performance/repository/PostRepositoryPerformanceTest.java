package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 PostRepository Performance Tests")
class PostRepositoryPerformanceTest {

    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 500;
    private static final int POSTS = 20_000;
    private Long meId;

    @BeforeEach
    void setUp() {
        if (postRepository.count() > 0) return;

        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@t.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        meId = users.get(0).getUserId();

        List<Post> posts = new ArrayList<>(POSTS);
        for (int i = 0; i < POSTS; i++) {
            User author = users.get(i % USERS);
            Post p = new Post();
            p.setUser(author);
            p.setTitle("Title " + i);
            p.setContent("Content " + i);
            p.setImageUrl("img/" + i);
            p.setCreatedAt(LocalDateTime.now().minusHours(i % 48));
            p.setViewsCount(i % 200);
            posts.add(p);

            if (posts.size() % 1000 == 0) {
                postRepository.saveAll(posts);
                posts.clear();
                em.flush(); em.clear();
            }
        }
        if (!posts.isEmpty()) {
            postRepository.saveAll(posts);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱ findAllByUser_UserName — performance")
    void findAllByUser_UserName_performance() {
        Pageable p = PageRequest.of(0, 20);

        StopWatch sw = new StopWatch("PostRepository.findAllByUser_UserName");

        sw.start("User feed page 1");
        postRepository.findAllByUser_UserName("u1", p);
        sw.stop();

        sw.start("User feed page 2");
        postRepository.findAllByUser_UserName("u1", PageRequest.of(1, 20));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findAllByUser_UserName ==========");
        for (var t : sw.getTaskInfo()) System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ findAllByAnimalType — performance")
    void findAllByAnimalType_performance() {
        Pageable p = PageRequest.of(0, 20);

        StopWatch sw = new StopWatch("PostRepository.findAllByAnimalType");

        sw.start("Animal feed page 1");
        postRepository.findAllByAnimalType(null, p);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findAllByAnimalType ==========");
        for (var t : sw.getTaskInfo()) System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ sumViewsByUser — performance")
    void sumViewsByUser_performance() {
        StopWatch sw = new StopWatch("PostRepository.sumViewsByUser");

        sw.start("SUM views for user");
        postRepository.sumViewsByUser(meId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: sumViewsByUser ==========");
        for (var t : sw.getTaskInfo()) System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ findFollowingFeed — performance")
    void findFollowingFeed_performance() {
        Pageable p = PageRequest.of(0, 20);

        StopWatch sw = new StopWatch("PostRepository.findFollowingFeed");

        sw.start("Feed page 1");
        postRepository.findFollowingFeed(meId, p);
        sw.stop();

        sw.start("Feed page 2");
        postRepository.findFollowingFeed(meId, PageRequest.of(1, 20));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findFollowingFeed ==========");
        for (var t : sw.getTaskInfo()) System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}