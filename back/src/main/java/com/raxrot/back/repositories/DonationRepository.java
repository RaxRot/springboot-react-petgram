package com.raxrot.back.repositories;

import com.raxrot.back.models.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findAllByOrderByCreatedAtDesc();

    List<Donation> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

