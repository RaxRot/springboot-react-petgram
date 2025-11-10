package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Follow;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.FollowRepository;
import com.raxrot.back.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 FollowRepository Performance Tests")
class FollowRepositoryPerformanceTest {

    @Autowired private FollowRepository followRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 5_000;
    private static final int FOLLOWS = 100_000;

    private Long testUserId1;
    private Long testUserId2;

    @BeforeEach
    void setUp() {
        if (followRepository.count() > 0) return;

        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@mail.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        testUserId1 = users.get(0).getUserId();
        testUserId2 = users.get(1).getUserId();

        Set<String> usedPairs = new HashSet<>(FOLLOWS);
        List<Follow> batch = new ArrayList<>(2000);
        Random r = new Random();

        while (usedPairs.size() < FOLLOWS) {
            int a = r.nextInt(USERS);
            int b = r.nextInt(USERS);
            if (a == b) continue;

            String key = a + "-" + b;
            if (usedPairs.contains(key)) continue;

            usedPairs.add(key);

            Follow f = new Follow();
            f.setFollower(users.get(a));
            f.setFollowee(users.get(b));
            batch.add(f);

            if (batch.size() == 1000) {
                followRepository.saveAll(batch);
                batch.clear();
                em.flush(); em.clear();
            }
        }

        if (!batch.isEmpty()) {
            followRepository.saveAll(batch);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱ existsByFollower_UserIdAndFollowee_UserId — performance")
    void existsByFollower_UserIdAndFollowee_UserId_performance() {
        StopWatch sw = new StopWatch("FollowRepository.exists");

        sw.start("exists check");
        followRepository.existsByFollower_UserIdAndFollowee_UserId(testUserId1, testUserId2);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: existsByFollower_UserIdAndFollowee_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ countByFollowee_UserId — performance")
    void countByFollowee_UserId_performance() {
        StopWatch sw = new StopWatch("FollowRepository.countByFollowee");

        sw.start("count");
        followRepository.countByFollowee_UserId(testUserId1);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: countByFollowee_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ countByFollower_UserId — performance")
    void countByFollower_UserId_performance() {
        StopWatch sw = new StopWatch("FollowRepository.countByFollower");

        sw.start("count");
        followRepository.countByFollower_UserId(testUserId1);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: countByFollower_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ deleteByFollower_UserIdAndFollowee_UserId — performance")
    void deleteByFollower_UserIdAndFollowee_UserId_performance() {
        StopWatch sw = new StopWatch("FollowRepository.deleteOne");

        sw.start("delete one");
        followRepository.deleteByFollower_UserIdAndFollowee_UserId(testUserId1, testUserId2);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: deleteByFollower_UserIdAndFollowee_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ deleteAllByFollower_UserId — performance")
    void deleteAllByFollower_UserId_performance() {
        StopWatch sw = new StopWatch("FollowRepository.deleteAllFollower");

        sw.start("delete all for follower");
        followRepository.deleteAllByFollower_UserId(testUserId1);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: deleteAllByFollower_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}