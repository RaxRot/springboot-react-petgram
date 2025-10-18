package com.raxrot.back.services;

import com.raxrot.back.dtos.DonationResponse;

import java.util.List;

public interface AdminService {
    public List<DonationResponse> getAllDonations();

    long countUsers();

    long countDonations();

    long countComments();

    long countPosts();
}
