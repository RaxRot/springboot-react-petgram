package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsResponse {
    private long totalPosts;
    private long totalLikes;
    private long totalComments;
    private long totalViews;
    private long totalPets;
    private long totalFollowers;
    private long totalFollowing;
    private long totalDonationsReceived;
}