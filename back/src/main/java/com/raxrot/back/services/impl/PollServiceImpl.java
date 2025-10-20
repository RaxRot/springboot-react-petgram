package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PollOptionResponse;
import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.PollOptionRepository;
import com.raxrot.back.repositories.PollRepository;
import com.raxrot.back.repositories.PollVoteRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.PollService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollVoteRepository voteRepository;
    private final PostRepository postRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public PollResponse createPoll(Long postId, PollRequest req) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to create poll for post ID {}", me.getUserName(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with ID {} not found while user '{}' tried to create a poll", postId, me.getUserName());
                    return new ApiException("Post not found", HttpStatus.NOT_FOUND);
                });

        // только автор поста может создать опрос
        if (!post.getUser().getUserId().equals(me.getUserId())) {
            log.warn("User '{}' tried to add poll to someone else's post ID {}", me.getUserName(), postId);
            throw new ApiException("You cannot add poll to someone else's post", HttpStatus.FORBIDDEN);
        }

        Poll poll = new Poll();
        poll.setPost(post);
        poll.setQuestion(req.getQuestion());
        pollRepository.save(poll);

        for (String text : req.getOptions()) {
            PollOption opt = new PollOption();
            opt.setPoll(poll);
            opt.setOptionText(text);
            optionRepository.save(opt);
            poll.getOptions().add(opt);
            log.debug("Added poll option '{}' for poll ID {}", text, poll.getId());
        }

        log.info("Poll created successfully by user '{}' for post ID {}", me.getUserName(), postId);
        return buildPollResponse(poll, false);
    }

    @Override
    public PollResponse getPoll(Long postId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' fetching poll for post ID {}", me.getUserName(), postId);

        Poll poll = pollRepository.findByPost_Id(postId)
                .orElseThrow(() -> {
                    log.error("Poll for post ID {} not found", postId);
                    return new ApiException("Poll not found", HttpStatus.NOT_FOUND);
                });

        boolean voted = voteRepository.existsByPoll_IdAndUser_UserId(poll.getId(), me.getUserId());
        log.info("User '{}' fetched poll ID {} (already voted: {})", me.getUserName(), poll.getId(), voted);

        return buildPollResponse(poll, voted);
    }

    @Transactional
    @Override
    public PollResponse vote(Long pollId, Long optionId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to vote in poll ID {} (option ID {})", me.getUserName(), pollId, optionId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> {
                    log.error("Poll with ID {} not found for voting", pollId);
                    return new ApiException("Poll not found", HttpStatus.NOT_FOUND);
                });

        if (voteRepository.existsByPoll_IdAndUser_UserId(pollId, me.getUserId())) {
            log.warn("User '{}' tried to vote twice in poll ID {}", me.getUserName(), pollId);
            throw new ApiException("You already voted", HttpStatus.CONFLICT);
        }

        PollOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> {
                    log.error("Option with ID {} not found in poll {}", optionId, pollId);
                    return new ApiException("Option not found", HttpStatus.NOT_FOUND);
                });

        option.setVotes(option.getVotes() + 1);
        optionRepository.save(option);

        PollVote vote = new PollVote();
        vote.setPoll(poll);
        vote.setUser(me);
        vote.setSelectedOption(option);
        voteRepository.save(vote);

        log.info("User '{}' successfully voted in poll ID {} (option '{}')", me.getUserName(), pollId, option.getOptionText());
        return buildPollResponse(poll, true);
    }

    private PollResponse buildPollResponse(Poll poll, boolean voted) {
        PollResponse response = new PollResponse();
        response.setPollId(poll.getId());
        response.setQuestion(poll.getQuestion());
        response.setOptions(
                poll.getOptions().stream()
                        .map(opt -> new PollOptionResponse(opt.getId(), opt.getOptionText(), opt.getVotes()))
                        .toList()
        );
        response.setVoted(voted);
        log.debug("Built poll response for poll ID {} (voted={})", poll.getId(), voted);
        return response;
    }

    @Transactional
    @Override
    public void deletePoll(Long postId) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to delete poll for post ID {}", me.getUserName(), postId);

        Poll poll = pollRepository.findByPost_Id(postId)
                .orElseThrow(() -> {
                    log.error("Poll for post ID {} not found during delete", postId);
                    return new ApiException("Poll not found", HttpStatus.NOT_FOUND);
                });

        if (!poll.getPost().getUser().getUserId().equals(me.getUserId())) {
            log.warn("User '{}' attempted to delete someone else's poll (post ID {})", me.getUserName(), postId);
            throw new ApiException("You cannot delete someone else's poll", HttpStatus.FORBIDDEN);
        }

        voteRepository.deleteAll(voteRepository.findByPoll_Id(poll.getId()));
        optionRepository.deleteAll(poll.getOptions());
        pollRepository.delete(poll);

        log.info("Poll for post ID {} successfully deleted by user '{}'", postId, me.getUserName());
    }
}
