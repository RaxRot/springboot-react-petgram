package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostRequest;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.Role;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("PostServiceImpl Tests")
class PostServiceImplTest {

    @Mock private ModelMapper modelMapper;
    @Mock private AuthUtil authUtil;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileUploadService fileUploadService;
    @Mock private MultipartFile file;

    @InjectMocks
    private PostServiceImpl postService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");
        user.setBanned(false);

        post = new Post();
        post.setId(10L);
        post.setTitle("Cute cat");
        post.setUser(user);
        post.setImageUrl("img/cat.jpg");
        post.setViewsCount(0);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should create post successfully")
    void should_create_post_successfully() {
        PostRequest req = new PostRequest("Cute cat", "sleepy cat", AnimalType.CAT);
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("image/png");
        given(fileUploadService.uploadFile(file)).willReturn("img/cat.png");

        Post mappedPost = new Post();
        mappedPost.setTitle("Cute cat");
        given(modelMapper.map(req, Post.class)).willReturn(mappedPost);
        given(postRepository.save(any(Post.class))).willReturn(mappedPost);
        given(modelMapper.map(any(Post.class), eq(PostResponse.class)))
                .willReturn(new PostResponse());

        PostResponse result = postService.createPost(file, req);

        assertThat(result).isNotNull();
        verify(fileUploadService).uploadFile(file);
        verify(postRepository).save(any(Post.class));
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user is banned")
    void should_throw_when_user_banned() {
        user.setBanned(true);
        given(authUtil.loggedInUser()).willReturn(user);

        PostRequest req = new PostRequest("a", "b", AnimalType.DOG);

        assertThatThrownBy(() -> postService.createPost(file, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User is banned")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when file is null or empty")
    void should_throw_when_file_null_or_empty() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(true);

        PostRequest req = new PostRequest("a", "b", AnimalType.DOG);

        assertThatThrownBy(() -> postService.createPost(file, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("File is empty")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when file is not an image")
    void should_throw_when_invalid_file_type() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("text/plain");

        PostRequest req = new PostRequest("a", "b", AnimalType.DOG);

        assertThatThrownBy(() -> postService.createPost(file, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only image files are allowed")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should get all posts successfully")
    void should_get_all_posts() {
        Post p1 = new Post();
        p1.setId(1L);
        Page<Post> page = new PageImpl<>(List.of(p1));
        given(postRepository.findAll(any(Pageable.class))).willReturn(page);
        given(modelMapper.map(any(Post.class), eq(PostResponse.class)))
                .willReturn(new PostResponse());

        PostPageResponse resp = postService.getAllPosts(0, 5, "id", "asc");

        assertThat(resp.getContent()).hasSize(1);
        verify(postRepository).findAll(any(Pageable.class));
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should get posts by username successfully")
    void should_get_posts_by_username() {
        Page<Post> page = new PageImpl<>(List.of(post));
        given(postRepository.findAllByUser_UserName(eq("vlad"), any(Pageable.class)))
                .willReturn(page);
        given(modelMapper.map(any(Post.class), eq(PostResponse.class)))
                .willReturn(new PostResponse());

        PostPageResponse resp = postService.getPostsByUsername("vlad", 0, 5, "id", "desc");

        assertThat(resp.getContent()).hasSize(1);
        verify(postRepository).findAllByUser_UserName(eq("vlad"), any(Pageable.class));
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch post by ID and increase views if not owner")
    void should_get_post_and_increase_views() {
        User another = new User();
        another.setUserId(2L);
        post.setUser(another);
        post.setViewsCount(5);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(authUtil.loggedInUser()).willReturn(user);

        postService.getPostById(10L);

        assertThat(post.getViewsCount()).isEqualTo(6);
        verify(postRepository).save(post);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found")
    void should_throw_when_post_not_found() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should delete post successfully by owner")
    void should_delete_post_successfully_by_owner() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deletePost(10L);

        verify(fileUploadService).deleteFile("img/cat.jpg");
        verify(postRepository).delete(post);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should delete post successfully by admin")
    void should_delete_post_successfully_by_admin() {
        User admin = new User();
        admin.setUserId(2L);
        admin.setUserName("admin");
        admin.setRoles(Set.of(new Role(1L, AppRole.ROLE_ADMIN)));
        given(authUtil.loggedInUser()).willReturn(admin);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deletePost(10L);

        verify(postRepository).delete(post);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found for deletion")
    void should_throw_when_post_not_found_delete() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when deleting another user's post without admin role")
    void should_throw_when_deleting_foreign_post() {
        User other = new User();
        other.setUserId(3L);
        post.setUser(other);

        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed to delete")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should handle exception when deleting image file")
    void should_handle_delete_file_exception() {
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        willThrow(new RuntimeException("fail")).given(fileUploadService).deleteFile(anyString());

        postService.deletePost(10L);

        verify(postRepository).delete(post);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch posts by animal type successfully")
    void should_get_posts_by_animal_type() {
        Page<Post> page = new PageImpl<>(List.of(post));
        given(postRepository.findAllByAnimalType(eq(AnimalType.CAT), any(Pageable.class)))
                .willReturn(page);
        given(modelMapper.map(any(Post.class), eq(PostResponse.class)))
                .willReturn(new PostResponse());

        PostPageResponse resp = postService.getPostsByAnimalType(AnimalType.CAT, 0, 5, "id", "asc");

        assertThat(resp.getContent()).hasSize(1);
        verify(postRepository).findAllByAnimalType(eq(AnimalType.CAT), any(Pageable.class));
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should get following feed successfully")
    void should_get_following_feed() {
        Page<Post> page = new PageImpl<>(List.of(post));
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findFollowingFeed(eq(1L), any(Pageable.class))).willReturn(page);
        given(modelMapper.map(any(Post.class), eq(PostResponse.class))).willReturn(new PostResponse());

        PostPageResponse resp = postService.getFollowingFeed(0, 5, "id", "desc");

        assertThat(resp.getContent()).hasSize(1);
        verify(postRepository).findFollowingFeed(eq(1L), any(Pageable.class));
    }
}
