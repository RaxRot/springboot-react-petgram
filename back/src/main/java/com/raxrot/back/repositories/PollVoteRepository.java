package com.raxrot.back.repositories;

import com.raxrot.back.models.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    boolean existsByPoll_IdAndUser_UserId(Long pollId, Long userId);

    Iterable<? extends PollVote> findByPoll_Id(Long id);
}
