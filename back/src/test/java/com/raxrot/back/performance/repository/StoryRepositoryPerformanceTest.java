package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Story;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.StoryRepository;
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
@DisplayName("🔬 StoryRepository Performance Tests")
class StoryRepositoryPerformanceTest {

    @Autowired private StoryRepository storyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;


    private static final int USERS = 500;
    private static final int FOLLOWEES_FOR_ME = 200;
    private static final int STORIES = 20_000;

    private Long meId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        if (userRepository.count() > 0 && storyRepository.count() > 0) {
            return;
        }

        now = LocalDateTime.now();


        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@t.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();


        meId = users.get(0).getUserId();


        var conn = em.unwrap(jakarta.persistence.EntityManager.class);

        for (int i = 1, inserted = 0; i < USERS && inserted < FOLLOWEES_FOR_ME; i++) {
            Long followeeId = users.get(i).getUserId();
            em.createNativeQuery(
                            "INSERT INTO tbl_follows (follower_id, followee_id, created_at) VALUES (?,?,?)")
                    .setParameter(1, meId)
                    .setParameter(2, followeeId)
                    .setParameter(3, now.minusMinutes(i % 60))
                    .executeUpdate();
            inserted++;
        }
        em.flush(); em.clear();


        List<Story> stories = new ArrayList<>(STORIES);
        for (int i = 0; i < STORIES; i++) {

            User author = users.get(i % USERS);


            boolean active = (i % 2 == 0);
            LocalDateTime exp = active ? now.plusHours(24 - (i % 6))
                    : now.minusHours((i % 6) + 1);

            Story s = new Story();
            s.setUser(author);
            s.setImageUrl("https://cdn/img/" + i + ".jpg");
            s.setExpiresAt(exp);
            s.setViewsCount((long) (i % 100));
            stories.add(s);


            if (stories.size() % 1000 == 0) {
                storyRepository.saveAll(stories);
                stories.clear();
                em.flush(); em.clear();
            }
        }
        if (!stories.isEmpty()) {
            storyRepository.saveAll(stories);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱️ findFollowingStories (pageable, sorted) — performance")
    void findFollowingStories_performance() {
        now = LocalDateTime.now();
        Pageable p1 = PageRequest.of(0, 20);
        Pageable p2 = PageRequest.of(1, 20);

        StopWatch sw = new StopWatch("StoryRepository.findFollowingStories");


        sw.start("Following stories — page 1");
        storyRepository.findFollowingStories(meId, now, p1);
        sw.stop();


        sw.start("Following stories — page 2");
        storyRepository.findFollowingStories(meId, now, p2);
        sw.stop();


        System.out.println("\n========== 📊 PERFORMANCE REPORT: findFollowingStories ==========");
        for (var task : sw.getTaskInfo()) {
            System.out.printf("%s → %d ms%n", task.getTaskName(), task.getTimeMillis());
        }
        long total = sw.getTotalTimeMillis();
        double avg = total / 2.0;
        System.out.println("------------------------------------------------------------");
        System.out.printf("✅ Average execution time: %.3f ms%n", avg);
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱️ findAllByUser_UserIdAndExpiresAtAfter — performance")
    void findAllByUserAndNotExpired_performance() {
        now = LocalDateTime.now();
        Pageable p = PageRequest.of(0, 20);


        Long targetUserId = meId + 1;

        StopWatch sw = new StopWatch("StoryRepository.findAllByUser_UserIdAndExpiresAtAfter");

        sw.start("User stories (active) — page 1");
        storyRepository.findAllByUser_UserIdAndExpiresAtAfter(targetUserId, now, p);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findAllByUser_UserIdAndExpiresAtAfter ==========");
        for (var task : sw.getTaskInfo()) {
            System.out.printf("%s → %d ms%n", task.getTaskName(), task.getTimeMillis());
        }
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱️ deleteByExpiresAtBefore — performance (bulk cleanup)")
    void deleteExpired_performance() {
        LocalDateTime cutoff = LocalDateTime.now();
        StopWatch sw = new StopWatch("StoryRepository.deleteByExpiresAtBefore");

        sw.start("Bulk delete expired");
        long deleted = storyRepository.deleteByExpiresAtBefore(cutoff);
        sw.stop();

        System.out.println("\n========== 🧹 PERFORMANCE REPORT: deleteByExpiresAtBefore ==========");
        for (var task : sw.getTaskInfo()) {
            System.out.printf("%s → %d ms%n", task.getTaskName(), task.getTimeMillis());
        }
        System.out.printf("🗑️  Deleted rows: %d%n", deleted);
        System.out.println("============================================================\n");
    }
}
