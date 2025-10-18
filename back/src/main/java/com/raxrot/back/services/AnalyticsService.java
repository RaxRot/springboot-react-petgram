package com.raxrot.back.services;

import com.raxrot.back.dtos.UserStatsResponse;

public interface AnalyticsService {
    public UserStatsResponse getMyStats();
}
