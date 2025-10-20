package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.*;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.*;
import com.raxrot.back.services.EmailService;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.UserService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final FileUploadService fileUploadService;
    private final AuthUtil authUtil;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final MessageRepository messageRepository;

    @Transactional
    @Override
    public UserResponse uploadImgProfilePic(MultipartFile file) {
        User user = authUtil.loggedInUser();
        log.info("User '{}' is updating profile picture", user.getUserName());

        if (file == null || file.isEmpty()) {
            log.warn("User '{}' attempted to upload an empty profile image", user.getUserName());
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        String oldUrl = user.getProfilePic();
        String profileUrl = fileUploadService.uploadFile(file);
        user.setProfilePic(profileUrl);
        User savedUser = userRepository.save(user);
        log.info("Profile picture updated successfully for user '{}'", user.getUserName());

        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                fileUploadService.deleteFile(oldUrl);
                log.debug("Deleted old profile picture for user '{}'", user.getUserName());
            } catch (Exception e) {
                log.warn("Failed to delete old profile picture for user '{}': {}", user.getUserName(), e.getMessage());
            }
        }
        return modelMapper.map(savedUser, UserResponse.class);
    }

    @Transactional
    @Override
    public void deleteUserById(Long userId) {
        log.info("Attempting to delete user with ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found for deletion", userId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == AppRole.ROLE_ADMIN);
        if (isAdmin) {
            log.warn("Attempt to delete ADMIN user ID {}", userId);
            throw new ApiException("Impossible to delete ADMIN", HttpStatus.CONFLICT);
        }

        messageRepository.deleteAllForUser(userId);
        followRepository.deleteAllByFollower_UserId(userId);
        followRepository.deleteAllByFollowee_UserId(userId);
        likeRepository.deleteAllByUser_UserId(userId);
        bookmarkRepository.deleteAllByUser_UserId(userId);
        commentRepository.deleteAllByAuthor_UserId(userId);
        postRepository.deleteAllByUser_UserId(userId);

        String profilePic = user.getProfilePic();
        if (profilePic != null && !profilePic.isBlank()) {
            try {
                fileUploadService.deleteFile(profilePic);
                log.debug("Deleted profile picture for user ID {}", userId);
            } catch (Exception e) {
                log.warn("Failed to delete profile picture for user ID {}: {}", userId, e.getMessage());
            }
        }

        userRepository.delete(user);
        log.info("User ID {} successfully deleted", userId);
    }

    @Override
    public UserPageResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching all users (page={}, size={}, sortBy={}, order={})", pageNumber, pageSize, sortBy, sortOrder);
        Sort sort = sortOrder.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);
        Page<User> userPage = userRepository.findAll(pageDetails);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());

        log.info("Fetched {} users", userPage.getTotalElements());
        UserPageResponse resp = new UserPageResponse();
        resp.setContent(userResponses);
        resp.setPageNumber(userPage.getNumber());
        resp.setPageSize(userPage.getSize());
        resp.setTotalElements(userPage.getTotalElements());
        resp.setTotalPages(userPage.getTotalPages());
        resp.setLastPage(userPage.isLast());
        return resp;
    }

    @Override
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user by ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", userId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user by email '{}'", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User with email '{}' not found", email);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public UserResponseForSearch getUserByUsername(String username) {
        log.info("Fetching user by username '{}'", username);
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> {
                    log.error("User with username '{}' not found", username);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });
        return modelMapper.map(user, UserResponseForSearch.class);
    }

    @Transactional
    @Override
    public void banUser(Long userId) {
        log.info("Banning user ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        if (isAdmin) {
            log.warn("Attempt to ban ADMIN user ID {}", userId);
            throw new ApiException("Admins cannot be banned", HttpStatus.CONFLICT);
        }

        user.setBanned(true);
        userRepository.save(user);
        log.info("User ID {} successfully banned", userId);
    }

    @Transactional
    @Override
    public void unbanUser(Long userId) {
        log.info("Unbanning user ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        user.setBanned(false);
        userRepository.save(user);
        log.info("User ID {} successfully unbanned", userId);
    }

    @Transactional
    @Override
    public UserResponse updateUsername(UpdateUsernameRequest request) {
        User me = authUtil.loggedInUser();
        String newUsername = request.getNewUsername();
        log.info("User '{}' attempting to update username to '{}'", me.getUserName(), newUsername);

        if (newUsername.equals(me.getUserName())) {
            log.warn("User '{}' tried to set the same username", me.getUserName());
            throw new ApiException("New username is the same as current", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByUserName(newUsername)) {
            log.warn("Username '{}' already exists", newUsername);
            throw new ApiException("Username is already taken", HttpStatus.CONFLICT);
        }

        me.setUserName(newUsername);
        User saved = userRepository.save(me);
        log.info("Username updated successfully for user '{}'", newUsername);

        sendEmailUsernameUpdated(me, newUsername);
        return modelMapper.map(saved, UserResponse.class);
    }

    @Transactional
    @Override
    public void updatePassword(ChangePasswordRequest request) {
        User me = authUtil.loggedInUser();
        log.info("User '{}' attempting to update password", me.getUserName());

        if (!passwordEncoder.matches(request.getCurrentPassword(), me.getPassword())) {
            log.warn("User '{}' entered incorrect current password", me.getUserName());
            throw new ApiException("Current password does not match", HttpStatus.BAD_REQUEST);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("User '{}' new password and confirmation do not match", me.getUserName());
            throw new ApiException("Confirm password does not match", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(request.getNewPassword(), me.getPassword())) {
            log.warn("User '{}' tried to reuse current password", me.getUserName());
            throw new ApiException("New password is the same as current", HttpStatus.CONFLICT);
        }

        me.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(me);
        log.info("Password updated successfully for user '{}'", me.getUserName());
    }

    @Override
    public void sendUsernameReminder(ForgotUsernameRequest request) {
        log.info("Processing username reminder for email '{}'", request.getEmail());
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            sendEmailRemindUsername(user);
            log.info("Username reminder email sent to '{}'", user.getEmail());
        });
    }

    private void sendEmailRemindUsername(User user) {
        log.debug("Sending username reminder to '{}'", user.getEmail());
        emailService.sendEmail(
                user.getEmail(),
                "🔑 Your PetGram username reminder",
                "Hello!\n\n" +
                        "We received a request to remind you of your PetGram username.\n\n" +
                        "👉 Your username is: " + user.getUserName() + "\n\n" +
                        "If you didn’t request this reminder, you can safely ignore this email. ⚠️\n\n" +
                        "— The PetGram Team 🐾"
        );
    }

    private void sendEmailUsernameUpdated(User me, String newUsername) {
        log.debug("Sending username update confirmation to '{}'", me.getEmail());
        emailService.sendEmail(
                me.getEmail(),
                "🔄 Your PetGram username has been updated!",
                "Hello " + me.getUserName() + "!\n\n" +
                        "✅ Your username has been successfully changed.\n\n" +
                        "👉 Your new username is: " + newUsername + "\n\n" +
                        "If you didn’t make this change, please contact our support immediately ⚠️\n\n" +
                        "— The PetGram Team 🐾"
        );
    }

    @Override
    public PublicUserResponse getPublicUserByUsername(String username) {
        log.info("Fetching public profile for username '{}'", username);
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> {
                    log.error("Public user '{}' not found", username);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        long followers = followRepository.countByFollowee_UserId(user.getUserId());
        long following = followRepository.countByFollower_UserId(user.getUserId());
        log.debug("User '{}' has {} followers and {} following", username, followers, following);

        PublicUserResponse resp = new PublicUserResponse();
        resp.setId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setProfilePic(user.getProfilePic());
        resp.setFollowers(followers);
        resp.setFollowing(following);
        resp.setBanned(user.isBanned());
        return resp;
    }
}
