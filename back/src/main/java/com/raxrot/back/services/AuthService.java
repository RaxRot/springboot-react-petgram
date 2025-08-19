package com.raxrot.back.services;

import com.raxrot.back.security.dto.LoginRequest;
import com.raxrot.back.security.dto.SignupRequest;
import com.raxrot.back.security.dto.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface AuthService {
    ResponseEntity<UserInfoResponse> authenticate(LoginRequest loginRequest);

    ResponseEntity<?> register(SignupRequest signUpRequest);

    ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication);

    ResponseEntity<?> signout();
}
