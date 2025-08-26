package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PublicUserResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UserRepository userRepository;

    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.getPublicUserByUsername(username));
    }

    //FIX LATER ADD TO SERVICE!!!!!!!!!!!!
    @GetMapping("/username/{username}/id")
    public ResponseEntity<Long> getUserIdByUsername(@PathVariable String username) {
        Long id = userRepository.findByUserName(username)
                .map(User::getUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(id);
    }
}
