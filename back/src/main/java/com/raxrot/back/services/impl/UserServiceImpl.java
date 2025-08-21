package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UpdateUsernameRequest;
import com.raxrot.back.dtos.UserPageResponse;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.dtos.UserResponseForSearch;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.UserService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final FileUploadService fileUploadService;
    private final AuthUtil authUtil;

    @Transactional
    @Override
    public UserResponse uploadImgProfilePic(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }
        User user=authUtil.loggedInUser();

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

    @Override
    public UserPageResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);
        Page<User> userPage = userRepository.findAll(pageDetails);


        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());

        UserPageResponse userPageResponse = new UserPageResponse();
        userPageResponse.setContent(userResponses);
        userPageResponse.setPageNumber(userPage.getNumber());
        userPageResponse.setPageSize(userPage.getSize());
        userPageResponse.setTotalElements(userPage.getTotalElements());
        userPageResponse.setTotalPages(userPage.getTotalPages());
        userPageResponse.setLastPage(userPage.isLast());
        return userPageResponse;
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user=userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public UserResponseForSearch getUserByUsername(String username) {
        User user=userRepository.findByUserName(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return modelMapper.map(user, UserResponseForSearch.class);
    }

    @Transactional
    @Override
    public void banUser(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (isAdmin) {
            throw new ApiException("Admins cannot be banned", HttpStatus.CONFLICT);
        }

        user.setBanned(true);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void unbanUser(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setBanned(false);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public UserResponse updateUsername(UpdateUsernameRequest request) {
        String newUsername = request.getNewUsername();
        User me = authUtil.loggedInUser();
        if (newUsername.equals(me.getUserName()))
            throw new ApiException("New username is the same as current", HttpStatus.BAD_REQUEST);
        if (userRepository.existsByUserName(newUsername))
            throw new ApiException("Username is already taken", HttpStatus.CONFLICT);

        me.setUserName(newUsername);
        User saved = userRepository.save(me);
        return modelMapper.map(saved, UserResponse.class);
    }
}
