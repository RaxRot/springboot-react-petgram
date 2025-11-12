package com.raxrot.back.performance.service;

import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Follow;
import com.raxrot.back.models.Role;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.FollowRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.impl.FollowServiceImpl;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FollowServicePerformanceTest {

    @Autowired
    private FollowServiceImpl followService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private FollowRepository followRepository;

    @MockBean
    private UserRepository userRepository;

    private ExecutorService executorService;
    private User testUser;
    private User targetUser;
    private Follow testFollow;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        // Setup test user
        Set<Role> roles = new HashSet<>();
        roles.add(new Role(1L, AppRole.ROLE_USER));

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setProfilePic("https://test.com/profile.jpg");
        testUser.setRoles(roles);

        // Setup target user
        targetUser = new User();
        targetUser.setUserId(2L);
        targetUser.setUserName("targetuser");
        targetUser.setEmail("target@example.com");
        targetUser.setPassword("password");
        targetUser.setProfilePic("https://test.com/target.jpg");
        targetUser.setRoles(roles);

        // Setup test follow relationship
        testFollow = new Follow();
        testFollow.setId(1L);
        testFollow.setFollower(testUser);
        testFollow.setFollowee(targetUser);
        testFollow.setCreatedAt(LocalDateTime.now());

        // Common mock setups
        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
    }

    @Test
    @DisplayName("➕ PERFORMANCE TEST: followUser - 500 concurrent follows")
    void performanceTest_followUser_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulFollows = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("➕ followUser Performance Test");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollower_UserIdAndFollowee_UserId(anyLong(), anyLong())).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(testFollow);

        stopWatch.start("500 Concurrent Follow Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    followService.followUser(2L);
                    successfulFollows.incrementAndGet();
                } catch (Exception e) {
                    // Ignore errors for performance test
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("➕ PERFORMANCE TEST RESULTS - followUser");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulFollows.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulFollows.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("➖ PERFORMANCE TEST: unfollowUser - 400 concurrent unfollows")
    void performanceTest_unfollowUser_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulUnfollows = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("➖ unfollowUser Performance Test");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(targetUser));

        stopWatch.start("400 Concurrent Unfollow Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    followService.unfollowUser(2L);
                    successfulUnfollows.incrementAndGet();
                } catch (Exception e) {
                    // Ignore errors for performance test
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("➖ PERFORMANCE TEST RESULTS - unfollowUser");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulUnfollows.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulUnfollows.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("👥 PERFORMANCE TEST: getFollowersCount - 1000 concurrent requests")
    void performanceTest_getFollowersCount_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("👥 getFollowersCount Performance Test");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(followRepository.countByFollowee_UserId(anyLong())).thenReturn(150L);

        stopWatch.start("1000 Concurrent Followers Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    followService.getFollowersCount(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("👥 PERFORMANCE TEST RESULTS - getFollowersCount");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔍 PERFORMANCE TEST: getFollowingCount - 1000 concurrent requests")
    void performanceTest_getFollowingCount_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🔍 getFollowingCount Performance Test");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(followRepository.countByFollower_UserId(anyLong())).thenReturn(85L);

        stopWatch.start("1000 Concurrent Following Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    followService.getFollowingCount(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔍 PERFORMANCE TEST RESULTS - getFollowingCount");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("✅ PERFORMANCE TEST: isFollowing - 1200 concurrent status checks")
    void performanceTest_isFollowing_1200Concurrent() throws InterruptedException {
        int numberOfRequests = 1200;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("✅ isFollowing Performance Test");

        when(followRepository.existsByFollower_UserIdAndFollowee_UserId(anyLong(), anyLong()))
                .thenReturn(true, false); // Alternate responses

        stopWatch.start("1200 Concurrent Following Status Checks");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    followService.isFollowing(2L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("✅ PERFORMANCE TEST RESULTS - isFollowing");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: Mixed Operations - sustained load 45 seconds")
    void loadTest_MixedOperations_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 45;
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger follows = new AtomicInteger(0);
        AtomicInteger unfollows = new AtomicInteger(0);
        AtomicInteger statusChecks = new AtomicInteger(0);
        AtomicInteger countChecks = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 Mixed Operations Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        // Setup mocks for mixed operations
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollower_UserIdAndFollowee_UserId(anyLong(), anyLong()))
                .thenReturn(true, false, true, false);
        when(followRepository.countByFollowee_UserId(anyLong())).thenReturn(150L);
        when(followRepository.countByFollower_UserId(anyLong())).thenReturn(85L);
        when(followRepository.save(any(Follow.class))).thenReturn(testFollow);

        stopWatch.start("Sustained Mixed Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    int operationCounter = 0;
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            switch (operationCounter % 4) {
                                case 0:
                                    followService.followUser(2L);
                                    follows.incrementAndGet();
                                    break;
                                case 1:
                                    followService.unfollowUser(3L);
                                    unfollows.incrementAndGet();
                                    break;
                                case 2:
                                    followService.isFollowing(2L);
                                    statusChecks.incrementAndGet();
                                    break;
                                case 3:
                                    followService.getFollowersCount(1L);
                                    countChecks.incrementAndGet();
                                    break;
                            }
                            totalOperations.incrementAndGet();
                            operationCounter++;

                            Thread.sleep(150); // ~6-7 RPS per thread
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            // Ignore business logic errors in load test
                        }
                    }
                }, executorService))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(workers).get(50, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            System.out.println("Load test completed");
        }
        stopWatch.stop();

        int totalOps = totalOperations.get();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔥 MIXED LOAD TEST RESULTS - All Operations");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱️  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("📈 Total Operations: " + totalOps);
        System.out.println("➕ Follows: " + follows.get());
        System.out.println("➖ Unfollows: " + unfollows.get());
        System.out.println("✅ Status Checks: " + statusChecks.get());
        System.out.println("👥 Count Checks: " + countChecks.get());
        System.out.println("🔥 Total RPS: " + String.format("%.2f", totalOps / (double) durationSeconds));
        System.out.println("📊 Operations Distribution:");
        System.out.println("   - Follows: " + String.format("%.1f%%", follows.get() * 100.0 / totalOps));
        System.out.println("   - Unfollows: " + String.format("%.1f%%", unfollows.get() * 100.0 / totalOps));
        System.out.println("   - Status Checks: " + String.format("%.1f%%", statusChecks.get() * 100.0 / totalOps));
        System.out.println("   - Count Checks: " + String.format("%.1f%%", countChecks.get() * 100.0 / totalOps));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("🚫 PERFORMANCE TEST: Edge Cases - self-follow and non-existing users")
    void performanceTest_EdgeCases_300Concurrent() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger handledExceptions = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🚫 Edge Cases Performance Test");

        // Mock self-follow scenario
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Mock non-existing user
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        stopWatch.start("300 Concurrent Edge Case Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    // Alternate between self-follow and non-existing user
                    if (i % 2 == 0) {
                        try {
                            followService.followUser(1L); // Self-follow
                        } catch (Exception e) {
                            handledExceptions.incrementAndGet();
                        }
                    } else {
                        try {
                            followService.followUser(999L); // Non-existing user
                        } catch (Exception e) {
                            handledExceptions.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🚫 PERFORMANCE TEST RESULTS - Edge Cases");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⚠️  Handled Exceptions: " + handledExceptions.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📊 Exception Rate: " + String.format("%.2f%%", (handledExceptions.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }
}