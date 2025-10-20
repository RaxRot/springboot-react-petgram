package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final DonationRepository donationRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    public List<DonationResponse> getAllDonations() {
        log.info("Fetching all donations ordered by creation date (descending).");
        List<DonationResponse> donations = donationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(d -> new DonationResponse(
                        d.getId(),
                        d.getDonor().getUserName(),
                        d.getReceiver().getUserName(),
                        d.getAmount(),
                        d.getCurrency(),
                        d.getCreatedAt()
                ))
                .toList();
        log.info("Fetched {} donations successfully.", donations.size());
        return donations;
    }

    public long countUsers() {
        long count = userRepository.count();
        log.info("Counted {} users.", count);
        return count;
    }

    public long countDonations() {
        long count = donationRepository.count();
        log.info("Counted {} donations.", count);
        return count;
    }

    public long countComments() {
        long count = commentRepository.count();
        log.info("Counted {} comments.", count);
        return count;
    }

    public long countPosts() {
        long count = postRepository.count();
        log.info("Counted {} posts.", count);
        return count;
    }
}
