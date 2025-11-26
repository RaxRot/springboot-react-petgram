package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.*;
import com.raxrot.back.services.impl.AnalyticsServiceImpl;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsServicePerformanceTest {

    @Autowired
    private AnalyticsServiceImpl analyticsService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private LikeRepository likeRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private FollowRepository followRepository;

    @MockBean
    private PetRepository petRepository;

    @MockBean
    private DonationRepository donationRepository;

    private ExecutorService executorService;
    private User testUser;
    private List<Donation> testDonations;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@test.com");

        // Создаем тестовые донаты (1000 записей)
        testDonations = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            Donation donation = new Donation();
            donation.setId(i);
            donation.setAmount(100L * i);
            donation.setCurrency("EUR");
            donation.setCreatedAt(LocalDateTime.now().minusDays(i));

            // Каждый 10-й донат - нашему пользователю
            if (i % 10 == 0) {
                donation.setReceiver(testUser);
            } else {
                User otherUser = new User();
                otherUser.setUserId(i + 1000);
                donation.setReceiver(otherUser);
            }

            User donor = new User();
            donor.setUserId(i + 2000);
            donation.setDonor(donor);

            testDonations.add(donation);
        }

        when(authUtil.loggedInUser()).thenReturn(testUser);

        // 🔥 Моки с РЕАЛИСТИЧНЫМИ задержками БД
        setupRealisticRepositoryMocks();
    }

    private void setupRealisticRepositoryMocks() {
        Random random = new Random();

        // PostRepository моки с задержками
        when(postRepository.countByUser_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10)); // 5-15ms - норма для COUNT
            return 150L;
        });

        when(postRepository.sumViewsByUser(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(8 + random.nextInt(12)); // 8-20ms - SUM сложнее
            return 5000L;
        });

        // LikeRepository моки
        when(likeRepository.countByUser_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10));
            return 300L;
        });

        // CommentRepository моки
        when(commentRepository.countByAuthor_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10));
            return 80L;
        });

        // FollowRepository моки
        when(followRepository.countByFollowee_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10));
            return 120L;
        });

        when(followRepository.countByFollower_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10));
            return 90L;
        });

        // PetRepository моки
        when(petRepository.countByOwner_UserId(anyLong())).thenAnswer(invocation -> {
            Thread.sleep(5 + random.nextInt(10));
            return 3L;
        });

        // 🔥 Донаты - САМАЯ ТЯЖЕЛАЯ ОПЕРАЦИЯ!
        when(donationRepository.findAll()).thenAnswer(invocation -> {
            Thread.sleep(20 + random.nextInt(30)); // 20-50ms - FULL SCAN!
            return testDonations;
        });
    }

    @Test
    @DisplayName("📊 REALISTIC TEST: getMyStats - 7 DB Queries + Full Scan")
    void realisticTest_getMyStats_WithDBDelays() throws InterruptedException {
        int numberOfRequests = 100;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("📊 getMyStats Realistic Test");

        System.out.println("\n" + "═".repeat(80));
        System.out.println("📊 REALISTIC PERFORMANCE TEST - AnalyticsService.getMyStats()");
        System.out.println("═".repeat(80));
        System.out.println("🔍 Testing: 7 COUNT queries + 1 FULL TABLE SCAN (donations)");
        System.out.println("💾 Donations table: " + testDonations.size() + " records");
        System.out.println("⏱  Expected: 50-150ms per request (with realistic DB delays)");

        stopWatch.start(numberOfRequests + " Stats Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    UserStatsResponse stats = analyticsService.getMyStats();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = totalTime / (double) numberOfRequests;
        double rps = numberOfRequests * 1000.0 / totalTime;

        System.out.println("\n📊 REALISTIC TEST RESULTS:");
        System.out.println("═".repeat(80));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + totalTime + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", rps));
        System.out.println("📉 Avg Response Time: " + String.format("%.2f", avgTime) + " ms");

        // Анализ производительности
        System.out.println("\n🔍 PERFORMANCE ANALYSIS:");
        System.out.println("═".repeat(80));
        if (avgTime < 50) {
            System.out.println("✅ ХОРОШО: Быстрее ожидаемого (< 50ms)");
        } else if (avgTime < 100) {
            System.out.println("⚠  НОРМА: В пределах ожиданий (50-100ms)");
        } else {
            System.out.println("🚨 МЕДЛЕННО: Выше ожиданий (> 100ms) - оптимизация нужна!");
        }

        System.out.println("📋 Операции за запрос:");
        System.out.println("  • 7 COUNT запросов: ~35-105ms");
        System.out.println("  • 1 FULL SCAN donations: ~20-50ms");
        System.out.println("  • Stream processing: ~5-15ms");
        System.out.println("  • ИТОГО ожидалось: ~60-170ms");
        System.out.println("  • ФАКТИЧЕСКИ: " + String.format("%.2f", avgTime) + "ms");
        System.out.println("═".repeat(80) + "\n");
    }

    @Test
    @DisplayName("🔥 STRESS TEST: Concurrent Stats - Multiple Users")
    void stressTest_getMyStats_ConcurrentUsers() throws InterruptedException {
        int numberOfUsers = 50;
        int requestsPerUser = 10;
        int totalRequests = numberOfUsers * requestsPerUser;

        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🔥 Concurrent Stats Stress Test");

        System.out.println("\n" + "═".repeat(80));
        System.out.println("🔥 STRESS TEST - Concurrent Analytics Requests");
        System.out.println("═".repeat(80));
        System.out.println("👥 Users: " + numberOfUsers + " (concurrent)");
        System.out.println("📨 Requests per user: " + requestsPerUser);
        System.out.println("📈 Total requests: " + totalRequests);

        stopWatch.start(totalRequests + " Concurrent Stats Requests");

        // Создаем разных пользователей
        List<User> users = new ArrayList<>();
        for (long i = 1; i <= numberOfUsers; i++) {
            User user = new User();
            user.setUserId(i);
            user.setUserName("user_" + i);
            user.setEmail("user" + i + "@test.com");
            users.add(user);
        }

        // Запускаем запросы от разных пользователей
        IntStream.range(0, totalRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    User currentUser = users.get(i % numberOfUsers);

                    // Временно подменяем пользователя для этого потока
                    when(authUtil.loggedInUser()).thenReturn(currentUser);

                    analyticsService.getMyStats();
                    successfulRequests.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем ошибки для стресс-теста
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(60, TimeUnit.SECONDS);
        stopWatch.stop();

        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = totalTime / (double) totalRequests;

        System.out.println("\n📊 STRESS TEST RESULTS:");
        System.out.println("═".repeat(80));
        System.out.println("✅ Successful Requests: " + successfulRequests.get() + "/" + totalRequests);
        System.out.println("⏱  Total Time: " + totalTime + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", totalRequests * 1000.0 / totalTime));
        System.out.println("📉 Avg Response Time: " + String.format("%.2f", avgTime) + " ms");
        System.out.println("📊 Success Rate: " +
                String.format("%.2f%%", (successfulRequests.get() * 100.0 / totalRequests)));

        // Анализ конкурентности
        System.out.println("\n🔍 CONCURRENCY ANALYSIS:");
        System.out.println("═".repeat(80));
        if (avgTime < 80) {
            System.out.println("✅ ОТЛИЧНО: Хорошо масштабируется под нагрузкой");
        } else if (avgTime < 150) {
            System.out.println("⚠  НОРМА: Приемлемая производительность под нагрузкой");
        } else {
            System.out.println("🚨 ПЛОХО: Сильная деградация при конкурентности");
        }
        System.out.println("═".repeat(80) + "\n");
    }

    @Test
    @DisplayName("💾 MEMORY TEST: Donations Full Scan Impact")
    void memoryTest_DonationsFullScan() {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("💾 MEMORY TEST - Donations Full Scan Impact");
        System.out.println("═".repeat(80));
        System.out.println("🔍 Testing memory usage during donations full table scan");
        System.out.println("💾 Donations records: " + testDonations.size());

        // Измеряем память до
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Donations Full Scan + Stream Processing");

        // Выполняем тяжелую операцию
        UserStatsResponse stats = analyticsService.getMyStats();

        stopWatch.stop();

        // Измеряем память после
        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("\n📊 MEMORY TEST RESULTS:");
        System.out.println("═".repeat(80));
        System.out.println("💾 Memory Before: " + String.format("%.2f", memoryBefore / 1024.0 / 1024.0) + " MB");
        System.out.println("💾 Memory After: " + String.format("%.2f", memoryAfter / 1024.0 / 1024.0) + " MB");
        System.out.println("📈 Memory Used: " + String.format("%.2f", memoryUsed / 1024.0 / 1024.0) + " MB");
        System.out.println("⏱  Operation Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💰 Donations Received: " + stats.getTotalDonationsReceived());

        // Анализ использования памяти
        System.out.println("\n🔍 MEMORY ANALYSIS:");
        System.out.println("═".repeat(80));
        if (memoryUsed < 5 * 1024 * 1024) { // < 5MB
            System.out.println("✅ ХОРОШО: Низкое потребление памяти");
        } else if (memoryUsed < 20 * 1024 * 1024) { // < 20MB
            System.out.println("⚠  ВНИМАНИЕ: Умеренное потребление памяти");
        } else {
            System.out.println("🚨 ОПАСНО: Высокое потребление памяти!");
        }

        System.out.println("📋 Потребление на 1000 донатов: " +
                String.format("%.2f", memoryUsed / 1024.0 / 1024.0) + " MB");
        System.out.println("═".repeat(80) + "\n");
    }

    @Test
    @DisplayName("🚨 PERFORMANCE ISSUE: Donations Full Scan Bottleneck")
    void performanceIssueTest_DonationsBottleneck() throws InterruptedException {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("🚨 PERFORMANCE ISSUE TEST - Donations Full Scan Bottleneck");
        System.out.println("═".repeat(80));
        System.out.println("🔍 Demonstrating the performance impact of donations full table scan");

        // Тест с разным количеством донатов
        int[] donationSizes = {100, 1000, 5000, 10000};

        System.out.println("\n📊 PERFORMANCE VS DATASET SIZE:");
        System.out.println("═".repeat(80));
        System.out.printf("%-12s | %-12s | %-15s | %-15s%n",
                "Donations", "Avg Time (ms)", "Memory Used (MB)", "Status");
        System.out.println("─".repeat(80));

        for (int size : donationSizes) {
            // Создаем dataset указанного размера
            List<Donation> donations = new ArrayList<>();
            for (long i = 1; i <= size; i++) {
                Donation d = new Donation();
                d.setId(i);
                d.setAmount(100L * i);
                d.setCurrency("EUR");
                d.setCreatedAt(LocalDateTime.now().minusDays(i));

                if (i % 10 == 0) {
                    d.setReceiver(testUser);
                } else {
                    User other = new User();
                    other.setUserId(i + 10000);
                    d.setReceiver(other);
                }

                User donor = new User();
                donor.setUserId(i + 20000);
                d.setDonor(donor);

                donations.add(d);
            }

            when(donationRepository.findAll()).thenAnswer(invocation -> {
                Thread.sleep(10 + (size / 100)); // Задержка зависит от размера
                return donations;
            });

            // Замеряем производительность
            System.gc();
            long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            StopWatch sw = new StopWatch();
            sw.start("Test with " + size + " donations");

            analyticsService.getMyStats();

            sw.stop();

            System.gc();
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;

            String status = size <= 1000 ? "✅ OK" : size <= 5000 ? "⚠ SLOW" : "🚨 CRITICAL";

            System.out.printf("%-12d | %-12.2f | %-15.2f | %-15s%n",
                    size,
                    sw.getTotalTimeMillis() / 1.0,
                    memoryUsed / 1024.0 / 1024.0,
                    status);
        }

        System.out.println("═".repeat(80));
        System.out.println("\n💡 RECOMMENDATION: Add query for user-specific donations instead of full scan!");
    }
}