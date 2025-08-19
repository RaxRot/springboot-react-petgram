package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final FileUploadService fileUploadService;

    @Override
    public UserResponse uploadImgProfilePic(MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String oldUrl = user.getProfilePic();

        String profileUrl = fileUploadService.uploadFile(file);
        user.setProfilePic(profileUrl);
        User savedUser = userRepository.save(user);

        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                fileUploadService.deleteFile(oldUrl);
            } catch (Exception ignored) {

            }
        }
        return modelMapper.map(savedUser, UserResponse.class);
    }

    @Transactional
    @Override
    public void deleteUserById(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == AppRole.ROLE_ADMIN);
        if (!isAdmin) {
            String profilePic = user.getProfilePic();
            if (profilePic != null && !profilePic.isBlank()) {
                try {
                    fileUploadService.deleteFile(profilePic);
                } catch (Exception ignored) {

                }
            }
            userRepository.delete(user);
        }else{
            throw new ApiException("Impossible to delete ADMIN", HttpStatus.CONFLICT);
        }
    }
}
