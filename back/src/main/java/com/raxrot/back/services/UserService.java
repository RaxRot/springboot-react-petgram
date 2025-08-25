package com.raxrot.back.services;

import com.raxrot.back.dtos.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse uploadImgProfilePic(MultipartFile file);
    void deleteUserById(Long userId);
    UserPageResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    UserResponseForSearch getUserByUsername(String username);
    void banUser(Long userId);
    void unbanUser(Long userId);
    UserResponse updateUsername(UpdateUsernameRequest request);
    void updatePassword(ChangePasswordRequest request);
    void sendUsernameReminder(ForgotUsernameRequest request);
    PublicUserResponse getPublicUserByUsername(String username);
}
