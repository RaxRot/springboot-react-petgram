package com.raxrot.back.performance.service;

import com.raxrot.back.models.Story;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.StoryRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.impl.StoryServiceImpl;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoryServicePerformanceTest {

    @Autowired
    private StoryServiceImpl storyService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private FileUploadService fileUploadService;

    @MockBean
    private StoryRepository storyRepository;

    private ExecutorService executorService;
    private User testUser;
    private Story testStory;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setBanned(false);

        testStory = new Story();
        testStory.setId(1L);
        testStory.setUser(testUser);
        testStory.setImageUrl("https://test.com/image.jpg");
        testStory.setCreatedAt(LocalDateTime.now());
        testStory.setExpiresAt(LocalDateTime.now().plusHours(24));
        testStory.setViewsCount(0L);

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(fileUploadService.uploadFile(any(MultipartFile.class))).thenReturn("https://test.com/uploaded.jpg");
    }

    @Test
    @DisplayName("🚀 PERFORMANCE TEST: viewStory - 1000 concurrent views")
    void performanceTest_viewStory_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🚀 viewStory Performance Test");

        when(storyRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testStory));

        stopWatch.start("1000 Concurrent Story Views");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    storyService.viewStory(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🚀 PERFORMANCE TEST RESULTS - viewStory");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📱 PERFORMANCE TEST: followingStories - 500 concurrent requests")
    void performanceTest_followingStories_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📱 followingStories Performance Test");

        Page<Story> storyPage = new PageImpl<>(List.of(testStory), PageRequest.of(0, 10), 1);
        when(storyRepository.findFollowingStories(anyLong(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(storyPage);

        stopWatch.start("500 Concurrent Following Stories Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    storyService.followingStories(0, 10);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📱 PERFORMANCE TEST RESULTS - followingStories");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: myStories - sustained load 30 seconds")
    void loadTest_myStories_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 30;
        AtomicInteger successfulRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 myStories Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        Page<Story> storyPage = new PageImpl<>(List.of(testStory), PageRequest.of(0, 10), 1);
        when(storyRepository.findAllByUser_UserIdAndExpiresAtAfter(anyLong(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(storyPage);

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            storyService.myStories(0, 10);
                            successfulRequests.incrementAndGet();
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }, executorService))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(workers).get(35, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            System.out.println("Load test completed with timeout");
        }
        stopWatch.stop();

        int totalRequests = successfulRequests.get();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔥 LOAD TEST RESULTS - myStories");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Requests: " + totalRequests);
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("⚡ PERFORMANCE TEST: getAllStories - 300 concurrent requests")
    void performanceTest_getAllStories_300Concurrent() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("⚡ getAllStories Performance Test");

        Page<Story> storyPage = new PageImpl<>(List.of(testStory, testStory, testStory), PageRequest.of(0, 10), 100);
        when(storyRepository.findAll(any(Pageable.class))).thenReturn(storyPage);

        stopWatch.start("300 Concurrent GetAll Stories Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    storyService.getAllStories(0, 10);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("⚡ PERFORMANCE TEST RESULTS - getAllStories");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }
}