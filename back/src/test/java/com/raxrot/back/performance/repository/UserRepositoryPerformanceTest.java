package com.raxrot.back.performance.repository;

import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 UserRepository Performance Tests")
class UserRepositoryPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    private static final int USERS_COUNT = 20_000;
    private String existingUserName;
    private String nonExistingUserName = "non_existing_user";

    @BeforeEach
    void setUp() {
        if (userRepository.count() > 0) return;

        for (int i = 1; i <= USERS_COUNT; i++) {
            String username = "user" + i;
            String email = "user" + i + "@test.com";
            userRepository.save(new User(username, email, "pwd"));
            if (i == USERS_COUNT / 2) {
                existingUserName = username;
            }
        }
    }

    @Test
    @DisplayName("⏱️ Performance of findByUserName with 20k users")
    void testFindByUserNamePerformance() {
        StopWatch sw = new StopWatch("UserRepository.findByUserName Performance");

        // ✅ Test existing user
        sw.start("Find existing username");
        userRepository.findByUserName(existingUserName);
        sw.stop();

        // ❌ Test non-existing user
        sw.start("Find non-existing username");
        userRepository.findByUserName(nonExistingUserName);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findByUserName ==========");

        for (var task : sw.getTaskInfo()) {
            System.out.printf("%s → %d ms%n", task.getTaskName(), task.getTimeMillis());
        }

        long total = sw.getTotalTimeMillis();
        double avg = total / 2.0;

        System.out.println("------------------------------------------------------------");
        System.out.printf("✅ Average execution time: %.3f ms%n", avg);
        System.out.println("============================================================\n");
    }

}
