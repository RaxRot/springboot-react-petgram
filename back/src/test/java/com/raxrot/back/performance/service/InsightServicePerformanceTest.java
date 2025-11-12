package com.raxrot.back.performance.service;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.enums.PetType;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.impl.InsightServiceImpl;
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

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InsightServicePerformanceTest {

    @Autowired
    private InsightServiceImpl insightService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private DonationRepository donationRepository;

    @MockBean
    private PetRepository petRepository;

    private ExecutorService executorService;
    private List<User> testUsers;
    private List<Post> testPosts;
    private List<Comment> testComments;
    private List<Donation> testDonations;
    private List<Pet> testPets;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);


        testUsers = Arrays.asList(
                createUser(1L, "john_doe", "john@test.com"),
                createUser(2L, "jane_smith", "jane@test.com"),
                createUser(3L, "bob_wilson", "bob@test.com"),
                createUser(4L, "alice_brown", "alice@test.com")
        );


        testPosts = Arrays.asList(
                createPost(1L, "Amazing Dog Story", 150L, 45, testUsers.get(0)),
                createPost(2L, "Cute Cat Photos", 200L, 78, testUsers.get(1)),
                createPost(3L, "Parrot Training Tips", 75L, 23, testUsers.get(2)),
                createPost(4L, "Rabbit Care Guide", 300L, 112, testUsers.get(3))
        );


        testComments = Arrays.asList(
                createComment(1L, "Great post!", testPosts.get(0), testUsers.get(1)),
                createComment(2L, "Thanks for sharing", testPosts.get(0), testUsers.get(2)),
                createComment(3L, "Very helpful", testPosts.get(0), testUsers.get(1)),
                createComment(4L, "Awesome photos!", testPosts.get(1), testUsers.get(0)),
                createComment(5L, "My cat does the same", testPosts.get(1), testUsers.get(3)),
                createComment(6L, "Good tips", testPosts.get(2), testUsers.get(1))
        );


        testDonations = Arrays.asList(
                createDonation(1L, 5000L, testUsers.get(0), testUsers.get(1)),
                createDonation(2L, 7500L, testUsers.get(1), testUsers.get(2)),
                createDonation(3L, 3000L, testUsers.get(0), testUsers.get(3)),
                createDonation(4L, 10000L, testUsers.get(2), testUsers.get(0))
        );


        testPets = Arrays.asList(
                createPet(1L, "Buddy", PetType.DOG, testUsers.get(0)),
                createPet(2L, "Whiskers", PetType.CAT, testUsers.get(0)),
                createPet(3L, "Polly", PetType.PARROT, testUsers.get(1)),
                createPet(4L, "Fluffy", PetType.RABBIT, testUsers.get(1)),
                createPet(5L, "Max", PetType.DOG, testUsers.get(1))
        );


        setupMockRepositories();
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        user.setUserName(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setProfilePic("https://test.com/profile" + id + ".jpg");
        return user;
    }

    private Post createPost(Long id, String title, Long views, int likes, User user) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setContent("Content for " + title);
        post.setViewsCount(views);
        post.setUser(user);
        post.setAnimalType(AnimalType.DOG);
        post.setCreatedAt(LocalDateTime.now().minusDays(id));


        List<Like> likeList = new ArrayList<>();
        for (int i = 0; i < likes; i++) {
            Like like = new Like();
            like.setPost(post);
            like.setUser(testUsers.get(i % testUsers.size()));
            likeList.add(like);
        }
        post.setLikes(likeList);

        return post;
    }

    private Comment createComment(Long id, String text, Post post, User author) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setText(text);
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now().minusHours(id));
        return comment;
    }

    private Donation createDonation(Long id, Long amount, User donor, User receiver) {
        Donation donation = new Donation();
        donation.setId(id);
        donation.setAmount(amount);
        donation.setDonor(donor);
        donation.setReceiver(receiver);
        donation.setCurrency("EUR");
        donation.setCreatedAt(LocalDateTime.now().minusDays(id));
        return donation;
    }

    private Pet createPet(Long id, String name, PetType type, User owner) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);
        pet.setType(type);
        pet.setBreed(type + " Breed");
        pet.setAge(3);
        pet.setDescription("Friendly " + type);
        pet.setOwner(owner);
        pet.setCreatedAt(LocalDateTime.now().minusDays(id));
        return pet;
    }

    private void setupMockRepositories() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);


        when(postRepository.findTopByCreatedAtBetweenOrderByLikesDesc(sevenDaysAgo, now))
                .thenReturn(testPosts.get(3));
        when(postRepository.findTopByOrderByViewsCountDesc())
                .thenReturn(testPosts.get(3));


        when(commentRepository.findAll()).thenReturn(testComments);


        when(donationRepository.findAllByCreatedAtBetween(thirtyDaysAgo, now))
                .thenReturn(testDonations);


        when(petRepository.findAll()).thenReturn(testPets);
    }

    @Test
    @DisplayName("🔥 PERFORMANCE TEST: getInsights - Cold vs Hot Cache")
    void performanceTest_getInsights_ColdVsHotCache() throws InterruptedException {
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🔥 getInsights Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔥 PERFORMANCE TEST - getInsights (Cold vs Hot Cache)");
        System.out.println("═".repeat(70));

        stopWatch.start("Cold Cache - First Call");
        Map<String, Object> firstResult = insightService.getInsights();
        stopWatch.stop();

        long coldCacheTime = stopWatch.getLastTaskTimeMillis();
        System.out.println("❄  Cold Cache (First Call): " + coldCacheTime + " ms");

        AtomicInteger successfulRequests = new AtomicInteger(0);
        stopWatch.start("Hot Cache - " + numberOfRequests + " Concurrent Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    insightService.getInsights();
                    successfulRequests.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(10, TimeUnit.SECONDS);
        stopWatch.stop();

        long hotCacheTotalTime = stopWatch.getLastTaskTimeMillis();
        double avgHotCacheTime = hotCacheTotalTime / (double) numberOfRequests;

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("❄  Cold Cache Time: " + coldCacheTime + " ms");
        System.out.println("🔥 Hot Cache Total Time: " + hotCacheTotalTime + " ms");
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful Requests: " + successfulRequests.get());
        System.out.println("📉 Avg Hot Cache Time: " + String.format("%.3f", avgHotCacheTime) + " ms");
        System.out.println("🚀 Performance Improvement: " + String.format("%.2f", coldCacheTime / avgHotCacheTime) + "x");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / hotCacheTotalTime));

        System.out.println("\n📋 INSIGHTS RESULTS:");
        System.out.println("═".repeat(70));
        firstResult.forEach((key, value) ->
                System.out.println("• " + key + ": " + value)
        );
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("⚡ PERFORMANCE TEST: calculateInsights - Heavy Operation")
    void performanceTest_calculateInsights_HeavyOperation() throws InterruptedException {
        int iterations = 50;
        StopWatch stopWatch = new StopWatch("⚡ calculateInsights Heavy Operation");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("⚡ PERFORMANCE TEST - calculateInsights (Heavy Operation)");
        System.out.println("═".repeat(70));

        stopWatch.start(iterations + " iterations of calculateInsights");

        for (int i = 0; i < iterations; i++) {
            insightService.calculateInsights();
        }

        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("🔄 Iterations: " + iterations);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("📉 Avg Time per Calculation: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) iterations) + " ms");
        System.out.println("💫 Calculations per Second: " +
                String.format("%.2f", iterations * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("🎯 LOAD TEST: Mixed Operations - 30 Second Sustained Load")
    void loadTest_MixedOperations_SustainedLoad() throws InterruptedException {
        int threads = 8;
        int durationSeconds = 30;
        AtomicInteger getInsightsCount = new AtomicInteger(0);
        AtomicInteger calculateInsightsCount = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🎯 Mixed Operations Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🎯 LOAD TEST - Mixed Operations (30 Second Sustained Load)");
        System.out.println("═".repeat(70));

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Random random = new Random();
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            if (random.nextDouble() < 0.8) {
                                // 80% - чтение из кэша
                                insightService.getInsights();
                                getInsightsCount.incrementAndGet();
                            } else {
                                // 20% - пересчет инсайтов
                                insightService.calculateInsights();
                                calculateInsightsCount.incrementAndGet();
                            }
                            Thread.sleep(100 + random.nextInt(200)); // Имитация нагрузки
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

        int totalOperations = getInsightsCount.get() + calculateInsightsCount.get();

        System.out.println("📊 LOAD TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("📖 getInsights Operations: " + getInsightsCount.get());
        System.out.println("🔄 calculateInsights Operations: " + calculateInsightsCount.get());
        System.out.println("📈 Total Operations: " + totalOperations);
        System.out.println("🔥 Total OPS: " + String.format("%.2f", totalOperations / (double) durationSeconds));
        System.out.println("📊 Read/Write Ratio: " +
                String.format("%.1f", getInsightsCount.get() / (double) calculateInsightsCount.get()) + ":1");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("💥 STRESS TEST: Concurrent Cache Access - 2000 Requests")
    void stressTest_ConcurrentCacheAccess() throws InterruptedException {
        int numberOfRequests = 2000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("💥 Concurrent Cache Stress Test");

        insightService.calculateInsights();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("💥 STRESS TEST - Concurrent Cache Access (2000 Requests)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Concurrent Cache Accesses");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    insightService.getInsights();
                    successfulRequests.incrementAndGet();
                } catch (Exception e) {
                    failedRequests.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(15, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 STRESS TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulRequests.get());
        System.out.println("❌ Failed: " + failedRequests.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📊 Success Rate: " +
                String.format("%.2f%%", (successfulRequests.get() * 100.0 / numberOfRequests)));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("📊 MEMORY TEST: Memory Usage During Insights Calculation")
    void memoryTest_InsightsCalculation() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("📊 MEMORY TEST - Memory Usage During Insights Calculation");
        System.out.println("═".repeat(70));

        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Insights Calculation with Memory Tracking");

        insightService.calculateInsights();

        stopWatch.stop();

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("📊 MEMORY USAGE RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("💾 Memory Before: " + String.format("%.2f", memoryBefore / 1024.0 / 1024.0) + " MB");
        System.out.println("💾 Memory After: " + String.format("%.2f", memoryAfter / 1024.0 / 1024.0) + " MB");
        System.out.println("📈 Memory Used: " + String.format("%.2f", memoryUsed / 1024.0 / 1024.0) + " MB");
        System.out.println("⏱  Calculation Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("⚡ Memory/Time Ratio: " +
                String.format("%.2f", memoryUsed / 1024.0 / stopWatch.getTotalTimeMillis()) + " KB/ms");
        System.out.println("═".repeat(70) + "\n");
    }
}