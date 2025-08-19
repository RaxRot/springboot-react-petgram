package com.raxrot.back.services;

import com.raxrot.back.dtos.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse uploadImgProfilePic(MultipartFile file, Authentication authentication);
    void deleteUserById(Long userId);
}
