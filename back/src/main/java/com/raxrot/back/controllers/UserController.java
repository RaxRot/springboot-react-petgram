package com.raxrot.back.controllers;

import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

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
}