package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.models.Poll;
import com.raxrot.back.models.PollOption;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PollOptionRepository;
import com.raxrot.back.repositories.PollRepository;
import com.raxrot.back.repositories.PollVoteRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.impl.PollServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PollServicePerformanceTest {

    @Autowired
    private PollServiceImpl pollService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private PollRepository pollRepository;

    @MockBean
    private PollOptionRepository pollOptionRepository;

    @MockBean
    private PollVoteRepository pollVoteRepository;

    @MockBean
    private PostRepository postRepository;

    private ExecutorService executorService;
    private User testUser;
    private Post testPost;
    private Poll testPoll;
    private PollOption testOption1;
    private PollOption testOption2;
    private PollRequest testPollRequest;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setUser(testUser);

        testPoll = new Poll();
        testPoll.setId(1L);
        testPoll.setPost(testPost);
        testPoll.setQuestion("Test Poll Question");

        testOption1 = new PollOption();
        testOption1.setId(1L);
        testOption1.setPoll(testPoll);
        testOption1.setOptionText("Option 1");
        testOption1.setVotes(5L);

        testOption2 = new PollOption();
        testOption2.setId(2L);
        testOption2.setPoll(testPoll);
        testOption2.setOptionText("Option 2");
        testOption2.setVotes(3L);

        testPoll.setOptions(List.of(testOption1, testOption2));

        testPollRequest = new PollRequest();
        testPollRequest.setQuestion("Favorite Animal?");
        testPollRequest.setOptions(List.of("Dog", "Cat", "Bird"));

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
    }

    @Test
    @DisplayName("🗳 PERFORMANCE TEST: getPoll - 800 concurrent requests")
    void performanceTest_getPoll_800Concurrent() throws InterruptedException {
        int numberOfRequests = 800;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🗳 getPoll Performance Test");

        when(pollRepository.findByPost_Id(anyLong())).thenReturn(Optional.of(testPoll));
        when(pollVoteRepository.existsByPoll_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);

        stopWatch.start("800 Concurrent Poll Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    pollService.getPoll(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🗳 PERFORMANCE TEST RESULTS - getPoll");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("✅ PERFORMANCE TEST: vote - 600 concurrent votes")
    void performanceTest_vote_600Concurrent() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulVotes = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("✅ vote Performance Test");

        when(pollRepository.findById(anyLong())).thenReturn(Optional.of(testPoll));
        when(pollVoteRepository.existsByPoll_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);
        when(pollOptionRepository.findById(1L)).thenReturn(Optional.of(testOption1));
        when(pollOptionRepository.findById(2L)).thenReturn(Optional.of(testOption2));

        stopWatch.start("600 Concurrent Votes");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    // Чередуем голоса между двумя вариантами
                    Long optionId = (i % 2 == 0) ? 1L : 2L;
                    pollService.vote(1L, optionId);
                    successfulVotes.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем конфликты голосования для перфоманс теста
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("✅ PERFORMANCE TEST RESULTS - vote");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful Votes: " + successfulVotes.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: getPoll - sustained load 30 seconds")
    void loadTest_getPoll_SustainedLoad() throws InterruptedException {
        int threads = 6;
        int durationSeconds = 30;
        AtomicInteger successfulRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 getPoll Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        when(pollRepository.findByPost_Id(anyLong())).thenReturn(Optional.of(testPoll));
        when(pollVoteRepository.existsByPoll_IdAndUser_UserId(anyLong(), anyLong())).thenReturn(false);

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            pollService.getPoll(1L);
                            successfulRequests.incrementAndGet();
                            Thread.sleep(300); // ~3 RPS per thread
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
        System.out.println("🔥 LOAD TEST RESULTS - getPoll");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Requests: " + totalRequests);
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("📊 PERFORMANCE TEST: createPoll - 200 concurrent creations")
    void performanceTest_createPoll_200Concurrent() throws InterruptedException {
        int numberOfRequests = 200;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulCreations = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("📊 createPoll Performance Test");

        when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));

        stopWatch.start("200 Concurrent Poll Creations");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    pollService.createPoll(1L, testPollRequest);
                    successfulCreations.incrementAndGet();
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
        System.out.println("📊 PERFORMANCE TEST RESULTS - createPoll");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulCreations.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulCreations.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }
}