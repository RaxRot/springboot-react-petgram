package com.raxrot.back.controllers;

import com.raxrot.back.dtos.ChangePasswordRequest;
import com.raxrot.back.dtos.UpdateUsernameRequest;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PatchMapping(value = "/uploadimg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadImg(@RequestParam("file") MultipartFile file) {
        log.info("🖼️ Profile image upload request received | fileName={}", file.getOriginalFilename());
        UserResponse response = userService.uploadImgProfilePic(file);
        log.info("✅ Profile image updated successfully | userId={} | username='{}'", response.getId(), response.getUserName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseForSearch> getUserByUsername(@PathVariable String username) {
        log.info("🔎 Fetching user by username='{}'", username);
        UserResponseForSearch userResponse = userService.getUserByUsername(username);
        log.info("✅ User found | username='{}' | profilePic={}", userResponse.getUserName(), userResponse.getProfilePic());
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/username")
    public ResponseEntity<UserResponse> updateUsername(@Valid @RequestBody UpdateUsernameRequest req) {
        log.info("✏️ Username update request received | newUsername='{}'", req.getNewUsername());
        UserResponse resp = userService.updateUsername(req);
        log.info("✅ Username updated successfully | userId={} | newUsername='{}'", resp.getId(), resp.getUserName());

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl old = (UserDetailsImpl) current.getPrincipal();
        log.debug("🔐 Refreshing SecurityContext for userId={}", old.getId());

        // build new principal
        UserDetailsImpl refreshed = new UserDetailsImpl(
                old.getId(),
                resp.getUserName(), // new username
                resp.getEmail(),
                old.getPassword(),
                old.getAuthorities()
        );

        // update SecurityContext
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(refreshed, current.getCredentials(), refreshed.getAuthorities());
        newAuth.setDetails(current.getDetails());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // new JWT cookie
        ResponseCookie jwtCookie = jwtUtils.getJwtCookie(refreshed);
        log.info("🍪 New JWT cookie generated for user='{}'", refreshed.getUsername());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(resp);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody ChangePasswordRequest req) {
        log.info("🔒 Password update request received for current user");
        userService.updatePassword(req);
        ResponseCookie clean = jwtUtils.getCleanJwtCookie();
        log.info("✅ Password updated and JWT cookie cleared");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clean.toString())
                .build();
    }
}
