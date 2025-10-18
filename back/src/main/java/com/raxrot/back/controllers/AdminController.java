package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.dtos.UserPageResponse;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.services.AdminService;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        UserPageResponse userPageResponse = userService.getAllUsers(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(userPageResponse, HttpStatus.OK);
    }
    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse userResponse = userService.getUserById(id);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse userResponse = userService.getUserByEmail(email);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PatchMapping("/ban/{id}")
    public ResponseEntity<Void>banUser(@PathVariable Long id) {
        userService.banUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PatchMapping("/unban/{id}")
    public ResponseEntity<Void>unBanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/donations")
    public ResponseEntity<List<DonationResponse>> getAllDonations() {
        return ResponseEntity.ok(adminService.getAllDonations());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
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
        return ResponseEntity.ok(stats);
    }
}
