package com.raxrot.back.performance.service;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServicePerformanceTest {

    @Autowired
    private AdminServiceImpl adminService;

    @MockBean
    private DonationRepository donationRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private PostRepository postRepository;

    @Autowired
    private ModelMapper modelMapper;

    private ExecutorService executorService;
    private User testUser1, testUser2;
    private Donation testDonation;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(15);

        // Setup test users
        Set<AppRole> roles = new HashSet<>();
        roles.add(AppRole.ROLE_USER);

        testUser1 = new User();
        testUser1.setUserId(1L);
        testUser1.setUserName("donor_user");
        testUser1.setEmail("donor@example.com");
        testUser1.setPassword("password");
        testUser1.setProfilePic("https://test.com/donor.jpg");

        testUser2 = new User();
        testUser2.setUserId(2L);
        testUser2.setUserName("receiver_user");
        testUser2.setEmail("receiver@example.com");
        testUser2.setPassword("password");
        testUser2.setProfilePic("https://test.com/receiver.jpg");

        // Setup test donation
        testDonation = new Donation();
        testDonation.setId(1L);
        testDonation.setDonor(testUser1);
        testDonation.setReceiver(testUser2);
        testDonation.setAmount(5000L);
        testDonation.setCurrency("USD");
        testDonation.setCreatedAt(LocalDateTime.now());

        // Setup test post
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setAnimalType(AnimalType.DOG);
        testPost.setUser(testUser1);
        testPost.setCreatedAt(LocalDateTime.now());

        // Setup test comment
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setText("This is a test comment");
        testComment.setPost(testPost);
        testComment.setAuthor(testUser2);
        testComment.setCreatedAt(LocalDateTime.now());

        // Common mock setups
        when(userRepository.count()).thenReturn(1500L);
        when(donationRepository.count()).thenReturn(250L);
        when(commentRepository.count()).thenReturn(5000L);
        when(postRepository.count()).thenReturn(1200L);
    }

    @Test
    @DisplayName("💰 PERFORMANCE TEST: getAllDonations - 400 concurrent requests")
    void performanceTest_getAllDonations_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("💰 getAllDonations Performance Test");

        // Mock donations list
        List<Donation> donations = List.of(
                testDonation, testDonation, testDonation, testDonation, testDonation,
                testDonation, testDonation, testDonation, testDonation, testDonation
        );

        when(donationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(donations);

        stopWatch.start("400 Concurrent Donation List Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    adminService.getAllDonations();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("💰 PERFORMANCE TEST RESULTS - getAllDonations");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Donations per Request: " + donations.size());
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("👥 PERFORMANCE TEST: countUsers - 600 concurrent requests")
    void performanceTest_countUsers_600Concurrent() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("👥 countUsers Performance Test");

        stopWatch.start("600 Concurrent User Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    adminService.countUsers();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("👥 PERFORMANCE TEST RESULTS - countUsers");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("👤 User Count: 1,500");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🎁 PERFORMANCE TEST: countDonations - 500 concurrent requests")
    void performanceTest_countDonations_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🎁 countDonations Performance Test");

        stopWatch.start("500 Concurrent Donation Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    adminService.countDonations();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🎁 PERFORMANCE TEST RESULTS - countDonations");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("💰 Donation Count: 250");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("💬 PERFORMANCE TEST: countComments - 700 concurrent requests")
    void performanceTest_countComments_700Concurrent() throws InterruptedException {
        int numberOfRequests = 700;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("💬 countComments Performance Test");

        stopWatch.start("700 Concurrent Comment Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    adminService.countComments();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("💬 PERFORMANCE TEST RESULTS - countComments");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("💭 Comment Count: 5,000");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📝 PERFORMANCE TEST: countPosts - 600 concurrent requests")
    void performanceTest_countPosts_600Concurrent() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📝 countPosts Performance Test");

        stopWatch.start("600 Concurrent Post Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    adminService.countPosts();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📝 PERFORMANCE TEST RESULTS - countPosts");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📄 Post Count: 1,200");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📊 PERFORMANCE TEST: All Count Methods - 800 concurrent mixed requests")
    void performanceTest_AllCountMethods_800Concurrent() throws InterruptedException {
        int numberOfRequests = 800;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger userCounts = new AtomicInteger(0);
        AtomicInteger donationCounts = new AtomicInteger(0);
        AtomicInteger commentCounts = new AtomicInteger(0);
        AtomicInteger postCounts = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("📊 All Count Methods Performance Test");

        stopWatch.start("800 Concurrent Mixed Count Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    switch (i % 4) {
                        case 0:
                            adminService.countUsers();
                            userCounts.incrementAndGet();
                            break;
                        case 1:
                            adminService.countDonations();
                            donationCounts.incrementAndGet();
                            break;
                        case 2:
                            adminService.countComments();
                            commentCounts.incrementAndGet();
                            break;
                        case 3:
                            adminService.countPosts();
                            postCounts.incrementAndGet();
                            break;
                    }
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📊 PERFORMANCE TEST RESULTS - All Count Methods");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📈 Requests Distribution:");
        System.out.println("   👥 User Counts: " + userCounts.get());
        System.out.println("   🎁 Donation Counts: " + donationCounts.get());
        System.out.println("   💬 Comment Counts: " + commentCounts.get());
        System.out.println("   📝 Post Counts: " + postCounts.get());
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: Admin Dashboard - sustained load 45 seconds")
    void loadTest_AdminDashboard_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 45;
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger donationLists = new AtomicInteger(0);
        AtomicInteger userCounts = new AtomicInteger(0);
        AtomicInteger donationCounts = new AtomicInteger(0);
        AtomicInteger commentCounts = new AtomicInteger(0);
        AtomicInteger postCounts = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 Admin Dashboard Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        // Setup mocks for mixed operations
        List<Donation> donations = List.of(testDonation, testDonation, testDonation);
        when(donationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(donations);

        stopWatch.start("Sustained Admin Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    int operationCounter = 0;
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            switch (operationCounter % 5) {
                                case 0:
                                    adminService.getAllDonations();
                                    donationLists.incrementAndGet();
                                    break;
                                case 1:
                                    adminService.countUsers();
                                    userCounts.incrementAndGet();
                                    break;
                                case 2:
                                    adminService.countDonations();
                                    donationCounts.incrementAndGet();
                                    break;
                                case 3:
                                    adminService.countComments();
                                    commentCounts.incrementAndGet();
                                    break;
                                case 4:
                                    adminService.countPosts();
                                    postCounts.incrementAndGet();
                                    break;
                            }
                            totalOperations.incrementAndGet();
                            operationCounter++;

                            Thread.sleep(200); // ~5 RPS per thread
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
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
        System.out.println("🔥 ADMIN DASHBOARD LOAD TEST RESULTS");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("📈 Total Operations: " + totalOps);
        System.out.println("💰 Donation Lists: " + donationLists.get());
        System.out.println("👤 User Counts: " + userCounts.get());
        System.out.println("🎁 Donation Counts: " + donationCounts.get());
        System.out.println("💬 Comment Counts: " + commentCounts.get());
        System.out.println("📝 Post Counts: " + postCounts.get());
        System.out.println("🔥 Total RPS: " + String.format("%.2f", totalOps / (double) durationSeconds));
        System.out.println("📊 Operations Distribution:");
        System.out.println("   - Donation Lists: " + String.format("%.1f%%", donationLists.get() * 100.0 / totalOps));
        System.out.println("   - User Counts: " + String.format("%.1f%%", userCounts.get() * 100.0 / totalOps));
        System.out.println("   - Donation Counts: " + String.format("%.1f%%", donationCounts.get() * 100.0 / totalOps));
        System.out.println("   - Comment Counts: " + String.format("%.1f%%", commentCounts.get() * 100.0 / totalOps));
        System.out.println("   - Post Counts: " + String.format("%.1f%%", postCounts.get() * 100.0 / totalOps));
        System.out.println("═".repeat(70) + "\n");
    }
}