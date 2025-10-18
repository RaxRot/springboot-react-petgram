package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final DonationRepository donationRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(d -> new DonationResponse(d.getId(),
                        d.getDonor().getUserName(),
                        d.getReceiver().getUserName(),
                        d.getAmount(), d.getCurrency(),
                        d.getCreatedAt()))
                .toList();
    }

    public long countUsers() {
        return userRepository.count();
    }

    public long countDonations() {
        return donationRepository.count();
    }

    public long countComments() {
        return commentRepository.count();
    }

    public long countPosts() {
        return postRepository.count();
    }
}