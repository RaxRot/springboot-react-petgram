package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 DonationRepository Performance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DonationRepositoryPerformanceTest {

    @Autowired private DonationRepository donationRepository;
    @Autowired private UserRepository userRepository;

    private static final int TOTAL_DONATIONS = 2000;
    private LocalDateTime from;
    private LocalDateTime to;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        userRepository.deleteAll();

        User donor1 = userRepository.save(new User("donor1", "donor1@mail.com", "pass"));
        User donor2 = userRepository.save(new User("donor2", "donor2@mail.com", "pass"));
        User receiver = userRepository.save(new User("receiver1", "receiver1@mail.com", "pass"));

        List<Donation> bulk = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < TOTAL_DONATIONS; i++) {
            Donation d = new Donation();
            d.setDonor(i % 2 == 0 ? donor1 : donor2);
            d.setReceiver(receiver);
            d.setAmount((long) (10 + i));
            d.setCurrency("EUR");
            d.setCreatedAt(now.minusDays(i % 30));
            bulk.add(d);
        }

        donationRepository.saveAll(bulk);

        from = now.minusDays(10);
        to = now;
    }

    private void measure(String name, Runnable r) {
        long start = System.currentTimeMillis();
        r.run();
        long end = System.currentTimeMillis();
        System.out.printf("%n========== 📊 PERFORMANCE REPORT: %s ==========%n%s → %d ms%n", name, name, (end - start));
    }

    @Test
    @Order(1)
    void performance_findAllByOrderByCreatedAtDesc() {
        measure("findAllByOrderByCreatedAtDesc", () ->
                donationRepository.findAllByOrderByCreatedAtDesc());
    }

    @Test
    @Order(2)
    void performance_findAllByCreatedAtBetween() {
        measure("findAllByCreatedAtBetween", () ->
                donationRepository.findAllByCreatedAtBetween(from, to));
    }
}