package com.raxrot.back.services;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;

public interface PollService {
    PollResponse createPoll(Long postId, PollRequest request);
    PollResponse getPoll(Long postId);
    PollResponse vote(Long pollId, Long optionId);
    void deletePoll(Long postId);
}
