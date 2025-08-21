package com.raxrot.back.controllers;

import com.raxrot.back.dtos.*;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.services.UserService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class UserController {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthUtil authUtil;

    @PatchMapping(value="/uploadimg", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadImg(
            @RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadImgProfilePic(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseForSearch>getUserByUsername(@PathVariable String username) {
        UserResponseForSearch userResponse=userService.getUserByUsername(username);
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/username")
    public ResponseEntity<UserResponse> updateUsername(@Valid @RequestBody UpdateUsernameRequest req) {
        UserResponse resp = userService.updateUsername(req);

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl old = (UserDetailsImpl) current.getPrincipal();

        // build new principal
        UserDetailsImpl refreshed = new UserDetailsImpl(
                old.getId(),
                resp.getUserName(),        // new username
                resp.getEmail(),
                old.getPassword(),
                old.getAuthorities()
        );

        // upd SecurityContext
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(refreshed, current.getCredentials(), refreshed.getAuthorities());
        newAuth.setDetails(current.getDetails());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // new jwt cookie
        ResponseCookie jwtCookie = jwtUtils.getJwtCookie(refreshed);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(resp);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void>updatePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.updatePassword(req);
        ResponseCookie clean = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clean.toString())
                .build();
    }
}