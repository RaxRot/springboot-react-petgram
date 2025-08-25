package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PublicUserResponse;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final UserService userService;

    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.getPublicUserByUsername(username));
    }
}
