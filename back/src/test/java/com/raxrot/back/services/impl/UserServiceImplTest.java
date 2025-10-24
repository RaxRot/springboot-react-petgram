package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.*;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Role;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.*;
import com.raxrot.back.services.EmailService;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ✅ Unit tests for UserServiceImpl.
 * Covers all core operations: upload, delete, ban, update username/password, reminder, and fetches.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private FileUploadService fileUploadService;
    @Mock private AuthUtil authUtil;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FollowRepository followRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private PostRepository postRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");
        user.setEmail("vlad@test.com");
        user.setPassword("encoded_pass");
        userRole = new Role(1L, AppRole.ROLE_USER);
        user.setRoles(Set.of(userRole));
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should upload profile picture successfully")
    void should_upload_profile_picture_successfully() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(fileUploadService.uploadFile(file)).willReturn("newUrl");
        given(authUtil.loggedInUser()).willReturn(user);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(modelMapper.map(any(User.class), eq(UserResponse.class)))
                .willReturn(new UserResponse(1L, "vlad", "vlad@test.com", "newUrl", false));

        UserResponse resp = userService.uploadImgProfilePic(file);

        assertThat(resp.getProfilePic()).isEqualTo("newUrl");
        verify(fileUploadService).uploadFile(file);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw when uploading empty profile picture")
    void should_throw_when_uploading_empty_file() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(true);
        given(authUtil.loggedInUser()).willReturn(user);

        assertThatThrownBy(() -> userService.uploadImgProfilePic(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("File is empty")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should delete user successfully")
    void should_delete_user_successfully() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.deleteUserById(1L);

        verify(messageRepository).deleteAllForUser(1L);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Should throw when user not found on delete")
    void should_throw_when_user_not_found_on_delete() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw when trying to delete admin user")
    void should_throw_when_deleting_admin() {
        Role adminRole = new Role(2L, AppRole.ROLE_ADMIN);
        user.setRoles(Set.of(adminRole));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteUserById(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Impossible to delete ADMIN")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should ban user successfully")
    void should_ban_user_successfully() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.banUser(1L);

        assertThat(user.isBanned()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw when banning admin user")
    void should_throw_when_banning_admin() {
        Role adminRole = new Role(2L, AppRole.ROLE_ADMIN);
        user.setRoles(Set.of(adminRole));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.banUser(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Admins cannot be banned");
    }

    @Test
    @DisplayName("Should unban user successfully")
    void should_unban_user_successfully() {
        user.setBanned(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.unbanUser(1L);

        assertThat(user.isBanned()).isFalse();
        verify(userRepository).save(user);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should update username successfully")
    void should_update_username_successfully() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(userRepository.existsByUserName("newName")).willReturn(false);
        given(userRepository.save(user)).willReturn(user);
        given(modelMapper.map(any(User.class), eq(UserResponse.class)))
                .willReturn(new UserResponse(1L, "newName", "vlad@test.com", null, false));

        UpdateUsernameRequest req = new UpdateUsernameRequest("newName");
        UserResponse resp = userService.updateUsername(req);

        assertThat(resp.getUserName()).isEqualTo("newName");
        verify(emailService).sendEmail(
                eq("vlad@test.com"),
                anyString(),
                argThat(body -> body.contains("username") || body.contains("changed"))
        );

    }

    @Test
    @DisplayName("Should throw when new username same as current")
    void should_throw_when_same_username() {
        given(authUtil.loggedInUser()).willReturn(user);

        UpdateUsernameRequest req = new UpdateUsernameRequest("vlad");
        assertThatThrownBy(() -> userService.updateUsername(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("same as current");
    }

    @Test
    @DisplayName("Should throw when username already exists")
    void should_throw_when_username_taken() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(userRepository.existsByUserName("taken")).willReturn(true);

        UpdateUsernameRequest req = new UpdateUsernameRequest("taken");
        assertThatThrownBy(() -> userService.updateUsername(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already taken");
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should update password successfully")
    void should_update_password_successfully() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(passwordEncoder.matches("old", "encoded_pass")).willReturn(true);
        given(passwordEncoder.matches("newPass", "encoded_pass")).willReturn(false);
        given(passwordEncoder.encode("newPass")).willReturn("encoded_new");

        ChangePasswordRequest req = new ChangePasswordRequest("old", "newPass", "newPass");
        userService.updatePassword(req);

        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("encoded_new");
    }

    @Test
    @DisplayName("Should throw when current password incorrect")
    void should_throw_when_wrong_current_password() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(passwordEncoder.matches("wrong", "encoded_pass")).willReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest("wrong", "new", "new");

        assertThatThrownBy(() -> userService.updatePassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("Should throw when confirm password mismatch")
    void should_throw_when_confirm_password_mismatch() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(passwordEncoder.matches("old", "encoded_pass")).willReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("old", "new", "diff");

        assertThatThrownBy(() -> userService.updatePassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Confirm password");
    }

    @Test
    @DisplayName("Should throw when new password same as current")
    void should_throw_when_reusing_password() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(passwordEncoder.matches("old", "encoded_pass")).willReturn(true);
        given(passwordEncoder.matches("new", "encoded_pass")).willReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("old", "new", "new");

        assertThatThrownBy(() -> userService.updatePassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("same as current");
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should send username reminder if email exists")
    void should_send_username_reminder_if_found() {
        given(userRepository.findByEmail("vlad@test.com")).willReturn(Optional.of(user));

        userService.sendUsernameReminder(new ForgotUsernameRequest("vlad@test.com"));

        verify(emailService).sendEmail(eq("vlad@test.com"), anyString(), contains("username"));
    }

    @Test
    @DisplayName("Should not send username reminder if email not found")
    void should_not_send_username_reminder_if_not_found() {
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        userService.sendUsernameReminder(new ForgotUsernameRequest("none@test.com"));

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch public user profile successfully")
    void should_fetch_public_user_profile_successfully() {
        given(userRepository.findByUserName("vlad")).willReturn(Optional.of(user));
        given(followRepository.countByFollowee_UserId(1L)).willReturn(5L);
        given(followRepository.countByFollower_UserId(1L)).willReturn(3L);

        PublicUserResponse resp = userService.getPublicUserByUsername("vlad");

        assertThat(resp.getUserName()).isEqualTo("vlad");
        assertThat(resp.getFollowers()).isEqualTo(5L);
        assertThat(resp.getFollowing()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should throw when public user not found")
    void should_throw_when_public_user_not_found() {
        given(userRepository.findByUserName("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPublicUserByUsername("ghost"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }
}
