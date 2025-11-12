package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Bookmark;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.BookmarkRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.impl.BookMarkServiceImpl;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
class BookMarkServicePerformanceTest {

    @Autowired
    private BookMarkServiceImpl bookMarkService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private BookmarkRepository bookmarkRepository;

    @MockBean
    private PostRepository postRepository;

    @Autowired
    private ModelMapper modelMapper;

    private ExecutorService executorService;
    private User testUser;
    private Post testPost;
    private Bookmark testBookmark;
    private PostResponse testPostResponse;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);

        // Setup test user
        Set<AppRole> roles = new HashSet<>();
        roles.add(AppRole.ROLE_USER);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setProfilePic("https://test.com/profile.jpg");

        // Setup test post
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post Title");
        testPost.setContent("This is a test post content");
        testPost.setImageUrl("https://test.com/post.jpg");
        testPost.setAnimalType(AnimalType.DOG);
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setUpdatedAt(LocalDateTime.now());
        testPost.setUser(testUser);
        testPost.setViewsCount(150L);

        // Setup test bookmark
        testBookmark = new Bookmark();
        testBookmark.setId(1L);
        testBookmark.setUser(testUser);
        testBookmark.setPost(testPost);
        testBookmark.setCreatedAt(LocalDateTime.now());

        // Setup test post response
        UserResponseForSearch userResponse = new UserResponseForSearch();
        userResponse.setUserName("testuser");
        userResponse.setProfilePic("https://test.com/profile.jpg");

        testPostResponse = new PostResponse();
        testPostResponse.setId(1L);
        testPostResponse.setTitle("Test Post Title");
        testPostResponse.setContent("This is a test post content");
        testPostResponse.setImageUrl("https://test.com/post.jpg");
        testPostResponse.setAnimalType(AnimalType.DOG);
        testPostResponse.setCreatedAt(LocalDateTime.now());
        testPostResponse.setUpdatedAt(LocalDateTime.now());
        testPostResponse.setUser(userResponse);
        testPostResponse.setViewsCount(150L);

        // Common mock setups
        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("🔖 PERFORMANCE TEST: addBookmark - 600 concurrent additions")
    void performanceTest_addBookmark_600Concurrent() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulAdditions = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🔖 addBookmark Performance Test");

        when(bookmarkRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(testBookmark);

        stopWatch.start("600 Concurrent Bookmark Additions");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    bookMarkService.addBookmark(1L);
                    successfulAdditions.incrementAndGet();
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
        System.out.println("🔖 PERFORMANCE TEST RESULTS - addBookmark");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulAdditions.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulAdditions.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🗑️ PERFORMANCE TEST: removeBookmark - 500 concurrent removals")
    void performanceTest_removeBookmark_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulRemovals = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🗑️ removeBookmark Performance Test");

        stopWatch.start("500 Concurrent Bookmark Removals");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    bookMarkService.removeBookmark(1L);
                    successfulRemovals.incrementAndGet();
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
        System.out.println("🗑️ PERFORMANCE TEST RESULTS - removeBookmark");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulRemovals.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulRemovals.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📚 PERFORMANCE TEST: getMyBookmarks - 800 concurrent requests with pagination")
    void performanceTest_getMyBookmarks_800Concurrent() throws InterruptedException {
        int numberOfRequests = 800;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📚 getMyBookmarks Performance Test");

        // Setup pagination mock
        Page<Bookmark> bookmarkPage = new PageImpl<>(
                List.of(testBookmark, testBookmark, testBookmark, testBookmark, testBookmark),
                PageRequest.of(0, 10, Sort.by("createdAt").descending()),
                25
        );

        when(bookmarkRepository.findAllByUser_UserId(anyLong(), any(Pageable.class))).thenReturn(bookmarkPage);

        stopWatch.start("800 Concurrent Bookmark List Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    // Test different pagination and sorting combinations
                    int page = i % 3; // 0, 1, 2
                    int size = 10 + (i % 3) * 5; // 10, 15, 20
                    String sortBy = i % 2 == 0 ? "createdAt" : "id";
                    String sortOrder = i % 2 == 0 ? "desc" : "asc";

                    bookMarkService.getMyBookmarks(page, size, sortBy, sortOrder);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📚 PERFORMANCE TEST RESULTS - getMyBookmarks");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Tested Variations:");
        System.out.println("   - Pages: 0, 1, 2");
        System.out.println("   - Sizes: 10, 15, 20");
        System.out.println("   - Sort Fields: createdAt, id");
        System.out.println("   - Sort Orders: asc, desc");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔄 PERFORMANCE TEST: Duplicate Bookmarks - 400 concurrent duplicate attempts")
    void performanceTest_duplicateBookmarks_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger duplicatePreventions = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🔄 Duplicate Bookmarks Performance Test");

        when(bookmarkRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(true);

        stopWatch.start("400 Concurrent Duplicate Bookmark Attempts");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    bookMarkService.addBookmark(1L);
                    // If no exception thrown, it means duplicate was handled gracefully
                    duplicatePreventions.incrementAndGet();
                } catch (Exception e) {
                    // Ignore other errors
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🔄 PERFORMANCE TEST RESULTS - Duplicate Bookmarks");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Duplicates Prevented: " + duplicatePreventions.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📊 Prevention Rate: " + String.format("%.2f%%", (duplicatePreventions.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: Mixed Bookmark Operations - sustained load 60 seconds")
    void loadTest_MixedBookmarkOperations_SustainedLoad() throws InterruptedException {
        int threads = 10;
        int durationSeconds = 60;
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger additions = new AtomicInteger(0);
        AtomicInteger removals = new AtomicInteger(0);
        AtomicInteger listRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 Mixed Bookmark Operations Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        // Setup mocks for mixed operations
        when(bookmarkRepository.existsByPost_IdAndUser_UserId(anyLong(), anyLong()))
                .thenReturn(false, true, false, true); // Alternate for realistic behavior
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(testBookmark);

        Page<Bookmark> bookmarkPage = new PageImpl<>(
                List.of(testBookmark, testBookmark),
                PageRequest.of(0, 10),
                15
        );
        when(bookmarkRepository.findAllByUser_UserId(anyLong(), any(Pageable.class))).thenReturn(bookmarkPage);

        stopWatch.start("Sustained Mixed Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    int operationCounter = 0;
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            switch (operationCounter % 3) {
                                case 0:
                                    bookMarkService.addBookmark(1L);
                                    additions.incrementAndGet();
                                    break;
                                case 1:
                                    bookMarkService.removeBookmark(1L);
                                    removals.incrementAndGet();
                                    break;
                                case 2:
                                    bookMarkService.getMyBookmarks(0, 10, "createdAt", "desc");
                                    listRequests.incrementAndGet();
                                    break;
                            }
                            totalOperations.incrementAndGet();
                            operationCounter++;

                            Thread.sleep(100); // ~10 RPS per thread
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
            CompletableFuture.allOf(workers).get(65, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            System.out.println("Load test completed");
        }
        stopWatch.stop();

        int totalOps = totalOperations.get();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔥 MIXED LOAD TEST RESULTS - Bookmark Operations");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱️  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("📈 Total Operations: " + totalOps);
        System.out.println("🔖 Additions: " + additions.get());
        System.out.println("🗑️ Removals: " + removals.get());
        System.out.println("📚 List Requests: " + listRequests.get());
        System.out.println("🔥 Total RPS: " + String.format("%.2f", totalOps / (double) durationSeconds));
        System.out.println("📊 Operations Distribution:");
        System.out.println("   - Additions: " + String.format("%.1f%%", additions.get() * 100.0 / totalOps));
        System.out.println("   - Removals: " + String.format("%.1f%%", removals.get() * 100.0 / totalOps));
        System.out.println("   - List Requests: " + String.format("%.1f%%", listRequests.get() * 100.0 / totalOps));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("🚫 PERFORMANCE TEST: Edge Cases - non-existing posts and errors")
    void performanceTest_EdgeCases_300Concurrent() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger handledExceptions = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🚫 Edge Cases Performance Test");

        stopWatch.start("300 Concurrent Edge Case Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    if (i % 2 == 0) {
                        // Non-existing post
                        try {
                            bookMarkService.addBookmark(999L);
                        } catch (Exception e) {
                            handledExceptions.incrementAndGet();
                        }
                    } else {
                        // Remove from non-existing post (should still work)
                        try {
                            bookMarkService.removeBookmark(999L);
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