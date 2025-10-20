package com.raxrot.back.controllers;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getMyStats() {
        log.info("User requested personal analytics statistics");

        UserStatsResponse stats = analyticsService.getMyStats();

        log.debug(
                "Stats fetched → posts={}, likes={}, comments={}, views={}, pets={}, followers={}, following={}, donations={}",
                stats.getTotalPosts(),
                stats.getTotalLikes(),
                stats.getTotalComments(),
                stats.getTotalViews(),
                stats.getTotalPets(),
                stats.getTotalFollowers(),
                stats.getTotalFollowing(),
                stats.getTotalDonationsReceived()
        );

        return ResponseEntity.ok(stats);
    }
}
