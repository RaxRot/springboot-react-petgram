package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.User;
import com.raxrot.back.services.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("test")
class ReportServicePerformanceTest {

    @Autowired
    private ReportServiceImpl reportService;

    private ExecutorService executorService;
    private User testUser;
    private UserStatsResponse testStats;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(15);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@mail.com");

        testStats = new UserStatsResponse();
        testStats.setTotalPosts(150L);
        testStats.setTotalLikes(2500L);
        testStats.setTotalComments(500L);
        testStats.setTotalViews(10000L);
        testStats.setTotalPets(3L);
        testStats.setTotalFollowers(450L);
        testStats.setTotalFollowing(380L);
        testStats.setTotalDonationsReceived(12500L); // 125.00 EUR
    }

    @Test
    @DisplayName("📊 PERFORMANCE TEST: generateUserStatsPdf - 50 concurrent generations")
    void performanceTest_generateUserStatsPdf_50Concurrent() throws InterruptedException {
        int numberOfRequests = 50;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulGenerations = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("📊 PDF Generation Performance Test");

        stopWatch.start("50 Concurrent PDF Generations");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(testUser, testStats);
                    if (pdfStream != null && pdfStream.available() > 0) {
                        successfulGenerations.incrementAndGet();
                        pdfStream.close();
                    }
                } catch (Exception e) {
                    System.err.println("PDF generation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(60, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("📊 PERFORMANCE TEST RESULTS - PDF Generation");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulGenerations.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulGenerations.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("💪 LOAD TEST: generateUserStatsPdf - sustained load 30 seconds")
    void loadTest_generateUserStatsPdf_SustainedLoad() throws InterruptedException {
        int threads = 5;
        int durationSeconds = 30;
        AtomicInteger successfulGenerations = new AtomicInteger(0);
        AtomicInteger failedGenerations = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("💪 PDF Generation Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
                        try {
                            ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(testUser, testStats);
                            if (pdfStream != null && pdfStream.available() > 0) {
                                successfulGenerations.incrementAndGet();
                                pdfStream.close();
                            } else {
                                failedGenerations.incrementAndGet();
                            }
                            Thread.sleep(1000); // 1 RPS per thread
                        } catch (Exception e) {
                            failedGenerations.incrementAndGet();
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

        int totalRequests = successfulGenerations.get() + failedGenerations.get();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("💪 LOAD TEST RESULTS - PDF Generation");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Successful: " + successfulGenerations.get());
        System.out.println("❌ Failed: " + failedGenerations.get());
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulGenerations.get() * 100.0 / totalRequests)));
        System.out.println("📄 Total PDFs Generated: " + successfulGenerations.get());
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("⚡ PERFORMANCE TEST: generateUserStatsPdf - 20 fast sequential generations")
    void performanceTest_generateUserStatsPdf_20FastSequential() {
        int numberOfRequests = 20;
        StopWatch stopWatch = new StopWatch("⚡ PDF Sequential Generation Test");
        AtomicInteger successfulGenerations = new AtomicInteger(0);

        stopWatch.start("20 Sequential PDF Generations");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            try {
                ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(testUser, testStats);
                if (pdfStream != null && pdfStream.available() > 0) {
                    successfulGenerations.incrementAndGet();
                    pdfStream.close();
                }
            } catch (Exception e) {
                System.err.println("Sequential PDF generation failed: " + e.getMessage());
            }
        });

        stopWatch.stop();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("⚡ PERFORMANCE TEST RESULTS - Sequential PDF Generation");
        System.out.println("═".repeat(70));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulGenerations.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🚀 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }
}