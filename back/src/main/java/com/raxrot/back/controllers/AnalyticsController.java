package com.raxrot.back.controllers;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getMyStats() {
        return ResponseEntity.ok(analyticsService.getMyStats());
    }
}

