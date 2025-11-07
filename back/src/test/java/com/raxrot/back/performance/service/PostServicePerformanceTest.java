package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.impl.PostServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServicePerformanceTest {

    @Autowired
    private PostServiceImpl postService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FileUploadService fileUploadService;

    private ExecutorService executorService;
    private User testUser;
    private Post testPost;
    private PostRequest testPostRequest;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setBanned(false);

        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setImageUrl("https://test.com/image.jpg");
        testPost.setUser(testUser);
        testPost.setViewsCount(10L);

        testPostRequest = new PostRequest();
        testPostRequest.setTitle("Test Post");
        testPostRequest.setContent("Test Content");

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(userRepository.findByUserName(anyString())).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadFile(any(MultipartFile.class))).thenReturn("https://test.com/uploaded.jpg");
    }

    @Test
    @DisplayName("📱 PERFORMANCE TEST: getPostById - 1000 concurrent views")
    void performanceTest_getPostById_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📱 getPostById Performance Test");

        when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));

        stopWatch.start("1000 Concurrent Post Views");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    postService.getPostById(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📱 PERFORMANCE TEST RESULTS - getPostById");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📄 PERFORMANCE TEST: getAllPosts - 500 concurrent requests")
    void performanceTest_getAllPosts_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📄 getAllPosts Performance Test");

        Page<Post> postPage = new PageImpl<>(List.of(testPost, testPost, testPost), PageRequest.of(0, 10), 100);
        when(postRepository.findAll(any(Pageable.class))).thenReturn(postPage);

        stopWatch.start("500 Concurrent GetAll Posts Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    postService.getAllPosts(0, 10, "createdAt", "desc");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📄 PERFORMANCE TEST RESULTS - getAllPosts");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: getPostsByUsername - sustained load 30 seconds")
    void loadTest_getPostsByUsername_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 30;
        AtomicInteger successfulRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 getPostsByUsername Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        Page<Post> postPage = new PageImpl<>(List.of(testPost), PageRequest.of(0, 10), 5);
        when(postRepository.findAllByUser_UserName(anyString(), any(Pageable.class))).thenReturn(postPage);

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            postService.getPostsByUsername("testuser", 0, 10, "createdAt", "desc");
                            successfulRequests.incrementAndGet();
                            Thread.sleep(200);
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
            System.out.println("Load test completed");
        }
        stopWatch.stop();

        int totalRequests = successfulRequests.get();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔥 LOAD TEST RESULTS - getPostsByUsername");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱️  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Requests: " + totalRequests);
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🐾 PERFORMANCE TEST: getPostsByAnimalType - 300 concurrent requests")
    void performanceTest_getPostsByAnimalType_300Concurrent() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🐾 getPostsByAnimalType Performance Test");

        Page<Post> postPage = new PageImpl<>(List.of(testPost), PageRequest.of(0, 10), 25);
        when(postRepository.findAllByAnimalType(any(), any(Pageable.class))).thenReturn(postPage);

        stopWatch.start("300 Concurrent Animal Type Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    postService.getPostsByAnimalType(AnimalType.DOG, 0, 10, "createdAt", "desc");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🐾 PERFORMANCE TEST RESULTS - getPostsByAnimalType");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("👥 PERFORMANCE TEST: getFollowingFeed - 400 concurrent requests")
    void performanceTest_getFollowingFeed_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("👥 getFollowingFeed Performance Test");

        Page<Post> postPage = new PageImpl<>(List.of(testPost, testPost), PageRequest.of(0, 10), 50);
        when(postRepository.findFollowingFeed(anyLong(), any(Pageable.class))).thenReturn(postPage);

        stopWatch.start("400 Concurrent Following Feed Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    postService.getFollowingFeed(0, 10, "createdAt", "desc");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("👥 PERFORMANCE TEST RESULTS - getFollowingFeed");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }
}