package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Bookmark;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.BookmarkRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("BookMarkServiceImpl Tests")
class BookMarkServiceImplTest {

    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private PostRepository postRepository;
    @Mock private AuthUtil authUtil;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private BookMarkServiceImpl bookMarkService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("vlad");

        post = new Post();
        post.setId(100L);
        post.setTitle("My Post");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should add bookmark successfully when not exists")
    void should_add_bookmark_successfully() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(bookmarkRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(false);

        // when
        bookMarkService.addBookmark(100L);

        // then
        verify(bookmarkRepository).save(any(Bookmark.class));
        verify(postRepository).findById(100L);
        verify(bookmarkRepository).existsByPost_IdAndUser_UserId(100L, 1L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should not add bookmark if it already exists")
    void should_not_add_when_already_exists() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(bookmarkRepository.existsByPost_IdAndUser_UserId(100L, 1L)).willReturn(true);

        // when
        bookMarkService.addBookmark(100L);

        // then
        verify(bookmarkRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw exception when post not found")
    void should_throw_exception_when_post_not_found() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);
        given(postRepository.findById(100L)).willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> bookMarkService.addBookmark(100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("🗑️ Should remove bookmark successfully")
    void should_remove_bookmark_successfully() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);

        // when
        bookMarkService.removeBookmark(100L);

        // then
        verify(bookmarkRepository).deleteByPost_IdAndUser_UserId(100L, 1L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return paginated bookmarks correctly (asc sort)")
    void should_return_paginated_bookmarks() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);

        Bookmark bm = new Bookmark();
        bm.setPost(post);

        Page<Bookmark> bmPage = new PageImpl<>(List.of(bm), PageRequest.of(0, 2, Sort.by("id")), 1);
        given(bookmarkRepository.findAllByUser_UserId(eq(1L), any(Pageable.class)))
                .willReturn(bmPage);

        PostResponse mappedResponse = new PostResponse();
        mappedResponse.setId(100L);
        mappedResponse.setTitle("My Post");
        given(modelMapper.map(post, PostResponse.class)).willReturn(mappedResponse);

        // when
        PostPageResponse result = bookMarkService.getMyBookmarks(0, 2, "id", "asc");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLastPage()).isTrue();

        verify(bookmarkRepository).findAllByUser_UserId(eq(1L), any(Pageable.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("📭 Should return empty list when no bookmarks exist")
    void should_return_empty_list_when_no_bookmarks() {
        // given
        given(authUtil.loggedInUser()).willReturn(user);

        // empty result → totalElements = 0 → totalPages = 0
        Page<Bookmark> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        given(bookmarkRepository.findAllByUser_UserId(eq(1L), any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        PostPageResponse response = bookMarkService.getMyBookmarks(0, 5, "id", "desc");

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(5);
    }
}