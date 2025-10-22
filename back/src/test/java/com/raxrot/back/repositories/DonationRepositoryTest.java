package com.raxrot.back.repositories;

import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DonationRepositoryTest {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    private User donor;
    private User receiver;

    private Donation donation1;
    private Donation donation2;
    private Donation donation3;

    @BeforeEach
    void setUp() {
        donor = userRepository.save(new User("donor", "donor@example.com", "pwd"));
        receiver = userRepository.save(new User("receiver", "receiver@example.com", "pwd"));

        LocalDateTime base = LocalDateTime.of(2025, 10, 22, 15, 0);

        donation1 = donationRepository.save(new Donation(null, donor, receiver, 50L, "USD", base.minusMinutes(10)));
        donation2 = donationRepository.save(new Donation(null, donor, receiver, 100L, "USD", base.minusMinutes(5)));
        donation3 = donationRepository.save(new Donation(null, donor, receiver, 25L, "USD", base));
    }

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc should return donations in descending order of creation")
    void findAllByOrderByCreatedAtDesc_ReturnsInDescendingOrder() {
        List<Donation> donations = donationRepository.findAllByOrderByCreatedAtDesc();

        assertThat(donations).hasSize(3);
        assertThat(donations.get(0).getAmount()).isEqualTo(25L);
        assertThat(donations.get(1).getAmount()).isEqualTo(100L);
        assertThat(donations.get(2).getAmount()).isEqualTo(50L);
    }
}
