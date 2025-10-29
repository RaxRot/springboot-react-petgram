package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.dtos.UserPageResponse;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.services.AdminService;
import com.raxrot.back.services.InsightsService;
import com.raxrot.back.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(
        name = "Admin Management",
        description = "Endpoints for administrators to manage users, donations, and platform statistics."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;
    private final InsightsService insightsService;

    @Operation(
            summary = "Get all users (paginated)",
            description = "Retrieve a paginated and sortable list of all registered users.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully fetched users",
                            content = @Content(schema = @Schema(implementation = UserPageResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden — only ADMIN can access this endpoint")
            }
    )
    @GetMapping
    public ResponseEntity<UserPageResponse> getAllUsers(
            @Parameter(description = "Page number (default = 0)")
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)")
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (default = userName)")
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @Parameter(description = "Sort direction: asc or desc (default = asc)")
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

        log.info("Admin requested user list (page={}, size={}, sortBy={}, sortOrder={})",
                pageNumber, pageSize, sortBy, sortOrder);
        UserPageResponse response = userService.getAllUsers(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Fetch details of a specific user using their unique ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("Admin fetching user by ID {}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(
            summary = "Get user by email",
            description = "Retrieve user information by email address.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        log.info("Admin fetching user by email '{}'", email);
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @Operation(
            summary = "Ban user",
            description = "Ban (block) a user by their ID. The user will no longer be able to log in.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully banned"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PatchMapping("/ban/{id}")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        log.warn("Admin attempting to ban user ID {}", id);
        userService.banUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Unban user",
            description = "Remove ban from a user so they can access their account again.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User successfully unbanned"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PatchMapping("/unban/{id}")
    public ResponseEntity<Void> unBanUser(@PathVariable Long id) {
        log.warn("Admin attempting to unban user ID {}", id);
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete user",
            description = "Permanently delete a user by ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.warn("Admin attempting to delete user ID {}", id);
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get all donations",
            description = "Retrieve a list of all donations made on the platform.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Donations successfully fetched",
                            content = @Content(schema = @Schema(implementation = DonationResponse.class)))
            }
    )
    @GetMapping("/donations")
    public ResponseEntity<List<DonationResponse>> getAllDonations() {
        log.info("Admin requested list of all donations");
        List<DonationResponse> donations = adminService.getAllDonations();
        return ResponseEntity.ok(donations);
    }

    @Operation(
            summary = "Get platform statistics",
            description = "Returns global statistics for users, donations, comments, and posts.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully fetched stats",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        log.info("Admin requested platform statistics");

        Map<String, Long> stats = Map.of(
                "users", adminService.countUsers(),
                "donations", adminService.countDonations(),
                "comments", adminService.countComments(),
                "posts", adminService.countPosts()
        );
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Get admin insights dashboard",
            description = "Returns daily platform insights: most liked post, top donor, and most active commenter.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Insights successfully fetched",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getAdminInsights() {
        log.info("📊 Admin requested insights dashboard data");
        Map<String, Object> insights = insightsService.getInsights();
        log.info("✅ Insights fetched successfully: {}", insights);
        return ResponseEntity.ok(insights);
    }
}
