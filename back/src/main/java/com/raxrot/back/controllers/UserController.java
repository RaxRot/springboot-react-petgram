package com.raxrot.back.controllers;

import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PatchMapping(value="/uploadimg", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadImg(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UserResponse response = userService.uploadImgProfilePic(file, authentication);
        return ResponseEntity.ok(response);
    }
}