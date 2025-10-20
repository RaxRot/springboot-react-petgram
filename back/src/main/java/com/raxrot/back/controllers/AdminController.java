package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.dtos.UserPageResponse;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.services.AdminService;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<UserPageResponse> getAllUsers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

        log.info("Admin requested user list (page={}, size={}, sortBy={}, sortOrder={})",
                pageNumber, pageSize, sortBy, sortOrder);
        UserPageResponse response = userService.getAllUsers(pageNumber, pageSize, sortBy, sortOrder);
        log.debug("Fetched {} users on page {}", response.getContent().size(), response.getPageNumber());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("Admin fetching user by ID {}", id);
        UserResponse userResponse = userService.getUserById(id);
        log.debug("Fetched user '{}'", userResponse.getUserName());
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        log.info("Admin fetching user by email '{}'", email);
        UserResponse userResponse = userService.getUserByEmail(email);
        log.debug("Fetched user '{}'", userResponse.getUserName());
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/ban/{id}")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        log.warn("Admin attempting to ban user ID {}", id);
        userService.banUser(id);
        log.info("User ID {} successfully banned", id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/unban/{id}")
    public ResponseEntity<Void> unBanUser(@PathVariable Long id) {
        log.warn("Admin attempting to unban user ID {}", id);
        userService.unbanUser(id);
        log.info("User ID {} successfully unbanned", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.warn("Admin attempting to delete user ID {}", id);
        userService.deleteUserById(id);
        log.info("User ID {} successfully deleted", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/donations")
    public ResponseEntity<List<DonationResponse>> getAllDonations() {
        log.info("Admin requested list of all donations");
        List<DonationResponse> donations = adminService.getAllDonations();
        log.debug("Fetched {} donations", donations.size());
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        log.info("Admin requested platform statistics");

        long users = adminService.countUsers();
        long donations = adminService.countDonations();
        long comments = adminService.countComments();
        long posts = adminService.countPosts();

        Map<String, Long> stats = Map.of(
                "users", users,
                "donations", donations,
                "comments", comments,
                "posts", posts
        );

        log.info("Admin stats → users={}, donations={}, comments={}, posts={}",
                users, donations, comments, posts);
        return ResponseEntity.ok(stats);
    }
}
