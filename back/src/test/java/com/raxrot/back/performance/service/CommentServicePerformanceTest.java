package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.*;
import com.raxrot.back.models.Comment;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.impl.CommentServiceImpl;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
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
class CommentServicePerformanceTest {

    @Autowired
    private CommentServiceImpl commentService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private PostRepository postRepository;

    private ExecutorService executorService;
    private User testUser;
    private Post testPost;
    private Comment testComment;
    private CommentRequest testCommentRequest;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setBanned(false);

        testPost = new Post();
        testPost.setId(1L);
        testPost.setUser(testUser);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setText("This is a test comment");
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setUpdatedAt(LocalDateTime.now());
        testComment.setPost(testPost);
        testComment.setAuthor(testUser);

        testCommentRequest = new CommentRequest();
        testCommentRequest.setText("New test comment");

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(postRepository.existsById(anyLong())).thenReturn(true);
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
    }

    @Test
    @DisplayName("💬 PERFORMANCE TEST: addComment - 600 concurrent comments")
    void performanceTest_addComment_600Concurrent() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulComments = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("💬 addComment Performance Test");

        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        stopWatch.start("600 Concurrent Comments");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    commentService.addComment(1L, testCommentRequest);
                    successfulComments.incrementAndGet();
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
        System.out.println("💬 PERFORMANCE TEST RESULTS - addComment");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulComments.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulComments.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📖 PERFORMANCE TEST: getComments - 1200 concurrent requests")
    void performanceTest_getComments_1200Concurrent() throws InterruptedException {
        int numberOfRequests = 1200;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📖 getComments Performance Test");

        Page<Comment> commentPage = new PageImpl<>(
                List.of(testComment, testComment, testComment, testComment, testComment),
                PageRequest.of(0, 10),
                25
        );
        when(commentRepository.findAllByPost_Id(anyLong(), any(Pageable.class))).thenReturn(commentPage);

        stopWatch.start("1200 Concurrent Comment Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    commentService.getComments(1L, 0, 10, "createdAt", "desc");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📖 PERFORMANCE TEST RESULTS - getComments");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("✏️ PERFORMANCE TEST: updateComment - 500 concurrent updates")
    void performanceTest_updateComment_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulUpdates = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("✏️ updateComment Performance Test");

        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        stopWatch.start("500 Concurrent Comment Updates");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    commentService.updateComment(1L, testCommentRequest);
                    successfulUpdates.incrementAndGet();
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
        System.out.println("✏️ PERFORMANCE TEST RESULTS - updateComment");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulUpdates.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulUpdates.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🗑️ PERFORMANCE TEST: deleteComment - 400 concurrent deletions")
    void performanceTest_deleteComment_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulDeletions = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🗑️ deleteComment Performance Test");

        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));

        stopWatch.start("400 Concurrent Comment Deletions");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    commentService.deleteComment(1L);
                    successfulDeletions.incrementAndGet();
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
        System.out.println("🗑️ PERFORMANCE TEST RESULTS - deleteComment");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulDeletions.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulDeletions.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📚 PERFORMANCE TEST: getAllComments - 1000 concurrent requests")
    void performanceTest_getAllComments_1000Concurrent() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📚 getAllComments Performance Test");

        Page<Comment> commentPage = new PageImpl<>(
                List.of(testComment, testComment, testComment),
                PageRequest.of(0, 10),
                150
        );
        when(commentRepository.findAll(any(Pageable.class))).thenReturn(commentPage);

        stopWatch.start("1000 Concurrent All Comments Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    commentService.getAllComments(0, 10, "createdAt", "desc");
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📚 PERFORMANCE TEST RESULTS - getAllComments");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: Mixed comment operations - sustained load 30 seconds")
    void loadTest_MixedCommentOperations_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 30;
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger adds = new AtomicInteger(0);
        AtomicInteger reads = new AtomicInteger(0);
        AtomicInteger updates = new AtomicInteger(0);
        AtomicInteger deletes = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 Mixed Comment Operations Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        Page<Comment> commentPage = new PageImpl<>(List.of(testComment, testComment), PageRequest.of(0, 10), 50);
        when(commentRepository.findAllByPost_Id(anyLong(), any(Pageable.class))).thenReturn(commentPage);
        when(commentRepository.findAll(any(Pageable.class))).thenReturn(commentPage);
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        stopWatch.start("Mixed Comment Operations - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            int operation = totalOperations.get() % 5;
                            switch (operation) {
                                case 0 -> {
                                    commentService.addComment(1L, testCommentRequest);
                                    adds.incrementAndGet();
                                }
                                case 1 -> {
                                    commentService.getComments(1L, 0, 10, "createdAt", "desc");
                                    reads.incrementAndGet();
                                }
                                case 2 -> {
                                    commentService.updateComment(1L, testCommentRequest);
                                    updates.incrementAndGet();
                                }
                                case 3 -> {
                                    commentService.deleteComment(1L);
                                    deletes.incrementAndGet();
                                }
                                case 4 -> {
                                    commentService.getAllComments(0, 10, "createdAt", "desc");
                                    reads.incrementAndGet();
                                }
                            }
                            totalOperations.incrementAndGet();
                            Thread.sleep(80); // ~12 RPS per thread
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

        int totalOps = totalOperations.get();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔥 LOAD TEST RESULTS - Mixed Comment Operations");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱️  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Operations: " + totalOps);
        System.out.println("💬 Adds: " + adds.get());
        System.out.println("📖 Reads: " + reads.get());
        System.out.println("✏️ Updates: " + updates.get());
        System.out.println("🗑️ Deletes: " + deletes.get());
        System.out.println("🔥 Total RPS: " + String.format("%.2f", totalOps / (double) durationSeconds));
        System.out.println("📊 Operations Distribution:");
        System.out.println("   - Add Comments: " + String.format("%.1f%%", adds.get() * 100.0 / totalOps));
        System.out.println("   - Read Comments: " + String.format("%.1f%%", reads.get() * 100.0 / totalOps));
        System.out.println("   - Update Comments: " + String.format("%.1f%%", updates.get() * 100.0 / totalOps));
        System.out.println("   - Delete Comments: " + String.format("%.1f%%", deletes.get() * 100.0 / totalOps));
        System.out.println("═".repeat(70) + "\n");
    }
}