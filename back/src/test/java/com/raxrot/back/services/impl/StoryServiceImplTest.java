package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.StoryResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.StoryRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("StoryServiceImpl Tests")
class StoryServiceImplTest {

    @Mock private AuthUtil authUtil;
    @Mock private FileUploadService fileUploadService;
    @Mock private StoryRepository storyRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private MultipartFile file;

    @InjectMocks
    private StoryServiceImpl storyService;

    private User user;
    private Story story;
    private StoryResponse storyResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");
        user.setBanned(false);

        story = new Story();
        story.setId(10L);
        story.setUser(user);
        story.setImageUrl("img/story.png");
        story.setCreatedAt(LocalDateTime.now().minusHours(1));
        story.setExpiresAt(LocalDateTime.now().plusHours(5));
        story.setViewsCount(3L);

        storyResponse = new StoryResponse(10L, "img/story.png", story.getCreatedAt(), 1L, "vlad");
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should create story successfully")
    void should_create_story_successfully() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("image/png");
        given(fileUploadService.uploadFile(file)).willReturn("img/story.png");
        given(storyRepository.save(any(Story.class))).willReturn(story);
        given(modelMapper.map(any(Story.class), eq(StoryResponse.class))).willReturn(storyResponse);

        StoryResponse result = storyService.create(file);

        assertThat(result.getImageUrl()).isEqualTo("img/story.png");
        verify(fileUploadService).uploadFile(file);
        verify(storyRepository).save(any(Story.class));
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user is banned")
    void should_throw_when_user_banned() {
        user.setBanned(true);
        given(authUtil.loggedInUser()).willReturn(user);

        assertThatThrownBy(() -> storyService.create(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User is banned")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when file is null or empty")
    void should_throw_when_file_null_or_empty() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(true);

        assertThatThrownBy(() -> storyService.create(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("File is empty")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when file type is not image")
    void should_throw_when_file_not_image() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("video/mp4");

        assertThatThrownBy(() -> storyService.create(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only image files are allowed")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should return user's active stories successfully")
    void should_get_my_stories() {
        Page<Story> page = new PageImpl<>(List.of(story));
        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findAllByUser_UserIdAndExpiresAtAfter(eq(1L), any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(page);
        given(modelMapper.map(any(Story.class), eq(StoryResponse.class))).willReturn(storyResponse);

        Page<StoryResponse> result = storyService.myStories(0, 5);

        assertThat(result.getContent()).hasSize(1);
        verify(storyRepository).findAllByUser_UserIdAndExpiresAtAfter(eq(1L), any(), any());
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should return following stories successfully")
    void should_get_following_stories() {
        Page<Story> page = new PageImpl<>(List.of(story));
        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findFollowingStories(eq(1L), any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(page);
        given(modelMapper.map(any(Story.class), eq(StoryResponse.class))).willReturn(storyResponse);

        Page<StoryResponse> result = storyService.followingStories(0, 5);

        assertThat(result.getContent()).hasSize(1);
        verify(storyRepository).findFollowingStories(eq(1L), any(), any());
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should delete story successfully by owner")
    void should_delete_story_successfully_by_owner() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findById(10L)).willReturn(Optional.of(story));

        storyService.delete(10L);

        verify(fileUploadService).deleteFile("img/story.png");
        verify(storyRepository).delete(story);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should delete story successfully by admin")
    void should_delete_story_successfully_by_admin() {
        User admin = new User();
        admin.setUserId(2L);
        admin.setUserName("admin");
        admin.setRoles(Set.of(new Role(1L, AppRole.ROLE_ADMIN)));

        given(authUtil.loggedInUser()).willReturn(admin);
        given(storyRepository.findById(10L)).willReturn(Optional.of(story));

        storyService.delete(10L);

        verify(storyRepository).delete(story);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when story not found during delete")
    void should_throw_when_story_not_found_delete() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> storyService.delete(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Story not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when deleting another user's story without admin rights")
    void should_throw_when_deleting_foreign_story() {
        User other = new User();
        other.setUserId(3L);
        story.setUser(other);

        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findById(10L)).willReturn(Optional.of(story));

        assertThatThrownBy(() -> storyService.delete(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed to delete")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should handle exception when deleting story file")
    void should_handle_delete_file_exception() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(storyRepository.findById(10L)).willReturn(Optional.of(story));
        willThrow(new RuntimeException("fail")).given(fileUploadService).deleteFile(anyString());

        storyService.delete(10L);

        verify(storyRepository).delete(story);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch all stories successfully")
    void should_get_all_stories() {
        Page<Story> page = new PageImpl<>(List.of(story));
        given(storyRepository.findAll(any(Pageable.class))).willReturn(page);
        given(modelMapper.map(any(Story.class), eq(StoryResponse.class))).willReturn(storyResponse);

        Page<StoryResponse> result = storyService.getAllStories(0, 5);

        assertThat(result.getContent()).hasSize(1);
        verify(storyRepository).findAll(any(Pageable.class));
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should view story successfully and increase views count")
    void should_view_story_and_increase_views() {
        User other = new User();
        other.setUserId(5L);
        story.setUser(other);
        story.setViewsCount(2L);

        given(storyRepository.findById(10L)).willReturn(Optional.of(story));
        given(authUtil.loggedInUser()).willReturn(user);
        given(modelMapper.map(any(Story.class), eq(StoryResponse.class))).willReturn(storyResponse);

        storyService.viewStory(10L);

        assertThat(story.getViewsCount()).isEqualTo(3L);
        verify(storyRepository).save(story);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when viewing non-existing story")
    void should_throw_when_viewing_non_existing_story() {
        given(storyRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> storyService.viewStory(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Story not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
