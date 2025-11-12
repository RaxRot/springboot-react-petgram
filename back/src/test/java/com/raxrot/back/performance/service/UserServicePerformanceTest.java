package com.raxrot.back.performance.service;

import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.impl.UserServiceImpl;
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

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServicePerformanceTest {

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private UserRepository userRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);

        // Мокаем базовые данные
        User testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@mail.com");
        testUser.setBanned(false);

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(userRepository.findByUserName(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("🚀 PERFORMANCE TEST: getUserByUsername - 1000 concurrent requests")
    void performanceTest_getUserByUsername_1000Concurrent() throws InterruptedException {
        // Arrange
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🚀 getUserByUsername Performance Test");

        // Act
        stopWatch.start("1000 Concurrent Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    userService.getUserByUsername("testuser");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        // Assert & Beautiful Output
        System.out.println("\n" + "═".repeat(60));
        System.out.println("🎯 PERFORMANCE TEST RESULTS - getUserByUsername");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: getUserByUsername - sustained load 30 seconds")
    void loadTest_getUserByUsername_SustainedLoad() throws InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        int threads = 10;
        int durationSeconds = 30;
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 getUserByUsername Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        // Act
        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            userService.getUserByUsername("testuser");
                            successfulRequests.incrementAndGet();
                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                        }
                        try {
                            Thread.sleep(50); // 20 RPS per thread
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }, executorService))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(workers).get(35, TimeUnit.SECONDS);
        stopWatch.stop();

        // Beautiful Output
        int totalRequests = successfulRequests.get() + failedRequests.get();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔥 LOAD TEST RESULTS - getUserByUsername");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Successful: " + successfulRequests.get());
        System.out.println("❌ Failed: " + failedRequests.get());
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulRequests.get() * 100.0 / totalRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("⚡ PERFORMANCE TEST: getUserById - 500 concurrent requests")
    void performanceTest_getUserById_500Concurrent() throws InterruptedException {
        // Arrange
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("⚡ getUserById Performance Test");

        // Act
        stopWatch.start("500 Concurrent Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    userService.getUserById(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        // Beautiful Output
        System.out.println("\n" + "═".repeat(60));
        System.out.println("🎯 PERFORMANCE TEST RESULTS - getUserById");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }
}