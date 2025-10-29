package com.raxrot.back.services;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.User;

import java.io.ByteArrayInputStream;

public interface ReportService {
    ByteArrayInputStream generateUserStatsPdf(User user, UserStatsResponse stats);
}
