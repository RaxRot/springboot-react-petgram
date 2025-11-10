package com.raxrot.back.performance.repository;

import com.raxrot.back.enums.PetType;
import com.raxrot.back.models.Pet;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 PetRepository Performance Tests")
class PetRepositoryPerformanceTest {

    @Autowired private PetRepository petRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 2_000;
    private static final int PETS = 20_000;

    private Long testUserId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        if (petRepository.count() > 0) return;

        // users
        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@mail.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        testUserId = users.get(0).getUserId();
        testUsername = users.get(0).getUserName();

        // pets
        List<Pet> pets = new ArrayList<>(PETS);
        for (int i = 0; i < PETS; i++) {
            User owner = users.get(i % USERS);
            Pet pet = new Pet();
            pet.setOwner(owner);
            pet.setName("Pet" + i);
            pet.setType(PetType.OTHER);
            pet.setAge(i % 15);
            pet.setBreed("Breed" + (i % 50));
            pets.add(pet);

            if (pets.size() % 1000 == 0) {
                petRepository.saveAll(pets);
                pets.clear();
                em.flush(); em.clear();
            }
        }

        if (!pets.isEmpty()) {
            petRepository.saveAll(pets);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱ findAllByOwner_UserId — performance")
    void findAllByOwner_UserId_performance() {
        Pageable p = PageRequest.of(0, 10);

        StopWatch sw = new StopWatch("PetRepository.findAllByOwner_UserId");

        sw.start("page 1");
        petRepository.findAllByOwner_UserId(testUserId, p);
        sw.stop();

        sw.start("page 2");
        petRepository.findAllByOwner_UserId(testUserId, PageRequest.of(1, 10));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findAllByOwner_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ findAllByOwner_UserName — performance")
    void findAllByOwner_UserName_performance() {
        Pageable p = PageRequest.of(0, 10);

        StopWatch sw = new StopWatch("PetRepository.findAllByOwner_UserName");

        sw.start("page 1");
        petRepository.findAllByOwner_UserName(testUsername, p);
        sw.stop();

        sw.start("page 2");
        petRepository.findAllByOwner_UserName(testUsername, PageRequest.of(1, 10));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findAllByOwner_UserName ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ countByOwner_UserId — performance")
    void countByOwner_UserId_performance() {
        StopWatch sw = new StopWatch("PetRepository.countByOwner_UserId");

        sw.start("count");
        petRepository.countByOwner_UserId(testUserId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: countByOwner_UserId ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}
