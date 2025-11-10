package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Like;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.LikeRepository;
import com.raxrot.back.repositories.PostRepository;
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
@DisplayName("🔬 LikeRepository Performance Tests")
class LikeRepositoryPerformanceTest {

    @Autowired private LikeRepository likeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 3_000;
    private static final int POSTS = 10_000;
    private static final int LIKES = 20_000;

    private Long testPostId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        if (likeRepository.count() > 0) return;

        // USERS
        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@mail.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        // POSTS
        List<Post> posts = new ArrayList<>(POSTS);
        for (int i = 0; i < POSTS; i++) {
            User owner = users.get(i % USERS);
            Post p = new Post();
            p.setUser(owner);
            p.setTitle("Title " + i);
            p.setContent("C " + i);
            p.setCreatedAt(LocalDateTime.now());
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

        List<Post> allPosts = postRepository.findAll();
        testPostId = allPosts.get(0).getId();
        testUserId = users.get(0).getUserId();

        // LIKES — уникальные (post,user)
        List<Like> likes = new ArrayList<>(LIKES);
        for (int i = 0; i < LIKES; i++) {
            User u = users.get(i % USERS);
            Post p = allPosts.get(i % allPosts.size());

            Like like = new Like();
            like.setUser(u);
            like.setPost(p);
            likes.add(like);

            if (likes.size() % 1000 == 0) {
                likeRepository.saveAll(likes);
                likes.clear();
                em.flush(); em.clear();
            }
        }

        if (!likes.isEmpty()) {
            likeRepository.saveAll(likes);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱ existsByPost_IdAndUser_UserId — performance")
    void existsByPost_IdAndUser_UserId_performance() {
        StopWatch sw = new StopWatch();

        sw.start("exists check");
        likeRepository.existsByPost_IdAndUser_UserId(testPostId, testUserId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: existsByPost_IdAndUser_UserId ==========");
        System.out.printf("%s → %d ms%n", sw.getLastTaskName(), sw.getLastTaskTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ countByPost_Id — performance")
    void countByPost_Id_performance() {
        StopWatch sw = new StopWatch();

        sw.start("count");
        likeRepository.countByPost_Id(testPostId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: countByPost_Id ==========");
        System.out.printf("%s → %d ms%n", sw.getLastTaskName(), sw.getLastTaskTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ countByUser_UserId — performance")
    void countByUser_UserId_performance() {
        StopWatch sw = new StopWatch();

        sw.start("count");
        likeRepository.countByUser_UserId(testUserId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: countByUser_UserId ==========");
        System.out.printf("%s → %d ms%n", sw.getLastTaskName(), sw.getLastTaskTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ deleteByPost_IdAndUser_UserId — performance")
    void deleteByPost_IdAndUser_UserId_performance() {
        StopWatch sw = new StopWatch();

        sw.start("delete one");
        likeRepository.deleteByPost_IdAndUser_UserId(testPostId, testUserId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: deleteByPost_IdAndUser_UserId ==========");
        System.out.printf("%s → %d ms%n", sw.getLastTaskName(), sw.getLastTaskTimeMillis());
        System.out.println("============================================================\n");
    }
}