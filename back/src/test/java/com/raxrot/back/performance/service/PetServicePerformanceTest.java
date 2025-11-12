package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.*;
import com.raxrot.back.enums.PetType;
import com.raxrot.back.models.Pet;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.impl.PetServiceImpl;
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
import org.springframework.web.multipart.MultipartFile;

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
class PetServicePerformanceTest {

    @Autowired
    private PetServiceImpl petService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private PetRepository petRepository;

    @MockBean
    private FileUploadService fileUploadService;

    private ExecutorService executorService;
    private User testUser;
    private Pet testPet;
    private PetRequest testPetRequest;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testuser");
        testUser.setProfilePic("https://test.com/profile.jpg");

        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setType(PetType.DOG);
        testPet.setBreed("Golden Retriever");
        testPet.setAge(3);
        testPet.setDescription("Friendly golden retriever");
        testPet.setPhotoUrl("https://test.com/pet.jpg");
        testPet.setOwner(testUser);
        testPet.setCreatedAt(LocalDateTime.now());

        testPetRequest = new PetRequest();
        testPetRequest.setName("Max");
        testPetRequest.setType(PetType.CAT);
        testPetRequest.setBreed("Siamese");
        testPetRequest.setAge(2);
        testPetRequest.setDescription("Playful siamese cat");

        when(authUtil.loggedInUser()).thenReturn(testUser);
        when(fileUploadService.uploadFile(any(MultipartFile.class))).thenReturn("https://test.com/uploaded.jpg");
    }

    @Test
    @DisplayName("🐶 PERFORMANCE TEST: getPetById - 700 concurrent requests")
    void performanceTest_getPetById_700Concurrent() throws InterruptedException {
        int numberOfRequests = 700;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🐶 getPetById Performance Test");

        when(petRepository.findById(anyLong())).thenReturn(Optional.of(testPet));

        stopWatch.start("700 Concurrent Pet Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    petService.getPetById(1L);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🐶 PERFORMANCE TEST RESULTS - getPetById");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🏠 PERFORMANCE TEST: getMyPets - 500 concurrent requests")
    void performanceTest_getMyPets_500Concurrent() throws InterruptedException {
        int numberOfRequests = 500;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🏠 getMyPets Performance Test");

        Page<Pet> petPage = new PageImpl<>(List.of(testPet, testPet, testPet), PageRequest.of(0, 10), 15);
        when(petRepository.findAllByOwner_UserId(anyLong(), any(Pageable.class))).thenReturn(petPage);

        stopWatch.start("500 Concurrent MyPets Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    petService.getMyPets(0, 10);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🏠 PERFORMANCE TEST RESULTS - getMyPets");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("🔥 LOAD TEST: getPetsByUsername - sustained load 30 seconds")
    void loadTest_getPetsByUsername_SustainedLoad() throws InterruptedException {
        int threads = 6;
        int durationSeconds = 30;
        AtomicInteger successfulRequests = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("🔥 getPetsByUsername Load Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        Page<Pet> petPage = new PageImpl<>(List.of(testPet), PageRequest.of(0, 10), 8);
        when(petRepository.findAllByOwner_UserName(anyString(), any(Pageable.class))).thenReturn(petPage);

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            petService.getPetsByUsername("testuser", 0, 10);
                            successfulRequests.incrementAndGet();
                            Thread.sleep(250); // ~4 RPS per thread
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
        System.out.println("🔥 LOAD TEST RESULTS - getPetsByUsername");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("⏱  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("✅ Total Requests: " + totalRequests);
        System.out.println("📈 Total RPS: " + String.format("%.2f", totalRequests / (double) durationSeconds));
        System.out.println("═".repeat(60) + "\n");
    }

    @Test
    @DisplayName("➕ PERFORMANCE TEST: createPet - 300 concurrent creations")
    void performanceTest_createPet_300Concurrent() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulCreations = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("➕ createPet Performance Test");

        when(petRepository.save(any(Pet.class))).thenReturn(testPet);

        stopWatch.start("300 Concurrent Pet Creations");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    petService.createPet(testPetRequest, null);
                    successfulCreations.incrementAndGet();
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
        System.out.println("➕ PERFORMANCE TEST RESULTS - createPet");
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

    @Test
    @DisplayName("🔄 PERFORMANCE TEST: updatePet - 400 concurrent updates")
    void performanceTest_updatePet_400Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulUpdates = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("🔄 updatePet Performance Test");

        when(petRepository.findById(anyLong())).thenReturn(Optional.of(testPet));
        when(petRepository.save(any(Pet.class))).thenReturn(testPet);

        stopWatch.start("400 Concurrent Pet Updates");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    petService.updatePet(1L, testPetRequest, null);
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
        System.out.println("🔄 PERFORMANCE TEST RESULTS - updatePet");
        System.out.println("═".repeat(60));
        System.out.println("📊 " + stopWatch.shortSummary());
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful: " + successfulUpdates.get());
        System.out.println("⏱  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("🔥 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response: " + String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " + String.format("%.2f%%", (successfulUpdates.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(60) + "\n");
    }
}