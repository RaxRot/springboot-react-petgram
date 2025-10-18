package com.raxrot.back.repositories;

import com.raxrot.back.models.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<Poll> findByPost_Id(Long postId);
}
