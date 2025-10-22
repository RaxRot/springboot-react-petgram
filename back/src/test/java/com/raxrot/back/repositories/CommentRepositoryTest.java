package com.raxrot.back.repositories;

import com.raxrot.back.models.Comment;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.enums.AnimalType;
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
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

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
        user1 = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "pwd"));

        post1 = postRepository.save(new Post(null, "Post 1", "Content 1", null, AnimalType.DOG, LocalDateTime.now(), LocalDateTime.now(), user1, null, null, null, 0));
        post2 = postRepository.save(new Post(null, "Post 2", "Content 2", null, AnimalType.CAT, LocalDateTime.now(), LocalDateTime.now(), user2, null, null, null, 0));

        commentRepository.save(new Comment(null, "Comment 1", LocalDateTime.now(), LocalDateTime.now(), post1, user1));
        commentRepository.save(new Comment(null, "Comment 2", LocalDateTime.now(), LocalDateTime.now(), post1, user2));
        commentRepository.save(new Comment(null, "Comment 3", LocalDateTime.now(), LocalDateTime.now(), post2, user1));
    }

    @Test
    @DisplayName("findAllByPost_Id should return all comments for a post in pageable")
    void findAllByPostId_ReturnsComments() {
        Page<Comment> page = commentRepository.findAllByPost_Id(post1.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("Comment 1");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Comment 2");
    }

    @Test
    @DisplayName("countByPost_Id should return correct count")
    void countByPostId_ReturnsCorrectCount() {
        long count = commentRepository.countByPost_Id(post1.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteAllByAuthor_UserId should remove all comments by a user")
    void deleteAllByAuthor_RemovesCommentsByUser() {
        commentRepository.deleteAllByAuthor_UserId(user1.getUserId());

        assertThat(commentRepository.countByAuthor_UserId(user1.getUserId())).isEqualTo(0);
        assertThat(commentRepository.countByPost_Id(post1.getId())).isEqualTo(1); // comment by user2 still exists
        assertThat(commentRepository.countByPost_Id(post2.getId())).isEqualTo(0); // comment by user1 removed
    }

    @Test
    @DisplayName("countByAuthor_UserId should return correct number of comments by user")
    void countByAuthor_ReturnsCorrectCount() {
        long count = commentRepository.countByAuthor_UserId(user1.getUserId());
        assertThat(count).isEqualTo(2);
    }
}
