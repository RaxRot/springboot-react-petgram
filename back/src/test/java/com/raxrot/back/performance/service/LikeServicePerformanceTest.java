package com.raxrot.back.performance.service;

import com.raxrot.back.models.Like;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.LikeRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.impl.LikeServiceImpl;
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
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LikeServicePerformanceTest {

    @Autowired
    private LikeServiceImpl likeService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private LikeRepository likeRepository;

    private ExecutorService executorService;
    private User testUser;
    private Post testPost;
    private Like testLike;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setUser(testUser);

        testLike = new Like();
        testLike.setId(1L);
        testLike.setPost(testPost);
        testLike.setUser(testUser);
        testLike.setCreatedAt(LocalDateTime.now());

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
    }

    @Test
    @DisplayName("💖 PERFORMANCE TEST: likePost - 1000 concurrent likes")
    void performanceTest_likePost_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulLikes = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("💖 likePost Performance Test");

        when(likeRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);

        stopWatch.start("1000 Concurrent Likes");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    likeService.likePost(1L);
                    successfulLikes.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем конфликты для перфоманс теста
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("💖 PERFORMANCE TEST RESULTS - likePost");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful Likes: " + successfulLikes.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulLikes.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("💔 PERFORMANCE TEST: unlikePost - 800 concurrent unlikes")
    void performanceTest_unlikePost_800Concurrent() throws InterruptedException {
        int numberOfRequests = 800;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulUnlikes = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("💔 unlikePost Performance Test");

        doNothing().when(likeRepository).deleteByPost_IdAndUser_UserId(anyLong(), anyLong());

        stopWatch.start("800 Concurrent Unlikes");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    likeService.unlikePost(1L);
                    successfulUnlikes.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем ошибки для перфоманс теста
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("💔 PERFORMANCE TEST RESULTS - unlikePost");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful Unlikes: " + successfulUnlikes.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulUnlikes.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📊 PERFORMANCE TEST: getLikesCount - 1200 concurrent requests")
    void performanceTest_getLikesCount_1200Concurrent() throws InterruptedException {
        int numberOfRequests = 1200;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📊 getLikesCount Performance Test");

        when(likeRepository.countByPost_Id(anyLong())).thenReturn(42L);

        stopWatch.start("1200 Concurrent Like Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    likeService.getLikesCount(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📊 PERFORMANCE TEST RESULTS - getLikesCount");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔍 PERFORMANCE TEST: isLikedByMe - 1500 concurrent checks")
    void performanceTest_isLikedByMe_1500Concurrent() throws InterruptedException {
        int numberOfRequests = 1500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🔍 isLikedByMe Performance Test");

        when(likeRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(true);

        stopWatch.start("1500 Concurrent Like Status Checks");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    likeService.isLikedByMe(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔍 PERFORMANCE TEST RESULTS - isLikedByMe");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: Mixed operations - sustained load 30 seconds")
    void loadTest_MixedOperations_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 30;
        AtomicInteger totalOperations = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 Mixed Like Operations Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        when(likeRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);
        when(likeRepository.countByPost_Id(anyLong())).thenReturn(100L);
        doNothing().when(likeRepository).deleteByPost_IdAndUser_UserId(anyLong(), anyLong());

        stopWatch.start("Mixed Operations - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            // Чередуем разные операции
                            int operation = totalOperations.get() % 4;
                            switch (operation) {
                                case 0 -> likeService.likePost(1L);
                                case 1 -> likeService.unlikePost(1L);
                                case 2 -> likeService.getLikesCount(1L);
                                case 3 -> likeService.isLikedByMe(1L);
                            }
                            totalOperations.incrementAndGet();
                            Thread.sleep(50); // ~20 RPS per thread
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            // Игнорируем ошибки для нагрузочного теста
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

        int operations = totalOperations.get();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔥 LOAD TEST RESULTS - Mixed Like Operations");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Operations: " + operations);
        System.out.println("📈 Total RPS: " + String.format("%.2f", operations / (double) durationSeconds));
        System.out.println("🔄 Operations Mix: Likes/Unlikes/Count/Status");
        System.out.println("═".repeat(60) + "\n");
    }
}