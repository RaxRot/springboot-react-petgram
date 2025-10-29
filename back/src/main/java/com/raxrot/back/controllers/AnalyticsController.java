package com.raxrot.back.controllers;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.User;
import com.raxrot.back.services.AnalyticsService;
import com.raxrot.back.services.ReportService;
import com.raxrot.back.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(
        name = "User Analytics",
        description = "Endpoints for retrieving personal statistics and analytics data for a logged-in user."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportService reportService;
    private final AuthUtil authUtil;

    @Operation(
            summary = "Get user statistics",
            description = "Returns analytical data and personal statistics of the currently logged-in user, such as total posts, likes, comments, and followers.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully fetched personal analytics",
                            content = @Content(schema = @Schema(implementation = UserStatsResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT token")
            }
    )
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

    @GetMapping("/stats/export")
    public ResponseEntity<byte[]> exportMyStatsAsPdf() {
        log.info("User requested PDF export of analytics report");

        User me = authUtil.loggedInUser();
        UserStatsResponse stats = analyticsService.getMyStats();
        ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(me, stats);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfStream.readAllBytes();
        } catch (Exception e) {
            log.error("Error reading generated PDF bytes: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=user_stats.pdf")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }

}
