package com.raxrot.back.repositories;

import com.raxrot.back.models.Bookmark;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User user1;
    private User user2;
    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        // Create users
        user1 = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "pwd"));

        // Create posts
        post1 = postRepository.save(new Post(null, "Post 1", "Content 1", null, null, LocalDateTime.now(), LocalDateTime.now(), user1, null, null, null, 0));
        post2 = postRepository.save(new Post(null, "Post 2", "Content 2", null, null, LocalDateTime.now(), LocalDateTime.now(), user2, null, null, null, 0));

        // Create bookmarks
        bookmarkRepository.save(new Bookmark(null, user1, post1, LocalDateTime.now()));
        bookmarkRepository.save(new Bookmark(null, user1, post2, LocalDateTime.now()));
        bookmarkRepository.save(new Bookmark(null, user2, post1, LocalDateTime.now()));
    }

    @Test
    @DisplayName("existsByPost_IdAndUser_UserId should check if a bookmark exists")
    void existsByPostIdAndUserId_WorksCorrectly() {
        boolean exists = bookmarkRepository.existsByPost_IdAndUser_UserId(post1.getId(), user1.getUserId());
        boolean notExists = bookmarkRepository.existsByPost_IdAndUser_UserId(post2.getId(), user2.getUserId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("findAllByUser_UserId should return paginated bookmarks for a user")
    void findAllByUserId_ReturnsBookmarks() {
        Page<Bookmark> bookmarksPage = bookmarkRepository.findAllByUser_UserId(user1.getUserId(), PageRequest.of(0, 10));

        assertThat(bookmarksPage.getTotalElements()).isEqualTo(2);
        assertThat(bookmarksPage.getContent().get(0).getPost().getTitle()).isEqualTo("Post 1");
        assertThat(bookmarksPage.getContent().get(1).getPost().getTitle()).isEqualTo("Post 2");
    }

    @Test
    @DisplayName("deleteByPost_IdAndUser_UserId should remove a specific bookmark")
    void deleteByPostIdAndUserId_RemovesBookmark() {
        bookmarkRepository.deleteByPost_IdAndUser_UserId(post1.getId(), user1.getUserId());

        boolean exists = bookmarkRepository.existsByPost_IdAndUser_UserId(post1.getId(), user1.getUserId());
        assertThat(exists).isFalse();
        assertThat(bookmarkRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteAllByUser_UserId should remove all bookmarks for a user")
    void deleteAllByUserId_RemovesAllBookmarks() {
        bookmarkRepository.deleteAllByUser_UserId(user1.getUserId());

        assertThat(bookmarkRepository.existsByPost_IdAndUser_UserId(post1.getId(), user1.getUserId())).isFalse();
        assertThat(bookmarkRepository.existsByPost_IdAndUser_UserId(post2.getId(), user1.getUserId())).isFalse();
        assertThat(bookmarkRepository.count()).isEqualTo(1); // user2's bookmark remains
    }
}
