package com.raxrot.back.performance.repository;

import com.raxrot.back.enums.AnimalType;
import com.raxrot.back.models.Comment;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("💬 CommentRepository Performance Tests")
class CommentRepositoryPerformanceTest {

    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;

    private Long testPostId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User("user_c", Math.random() + "@mail.com", "p");
        userRepository.save(u);
        testUserId = u.getUserId();

        Post p = new Post();
        p.setTitle("Test Post");
        p.setContent("Post for comments");
        p.setAnimalType(AnimalType.OTHER);
        p.setUser(u);
        postRepository.save(p);
        testPostId = p.getId();

        List<Comment> list = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Comment c = new Comment();
            c.setText("Comment " + i);
            c.setPost(p);
            c.setAuthor(u);
            list.add(c);
        }
        commentRepository.saveAll(list);

        commentRepository.countByPost_Id(testPostId);
    }

    private void print(String label, long ms) {
        System.out.println("========== 📊 " + label + " ==========");
        System.out.println(label + " → " + ms + " ms");
    }

    @Test @Order(1)
    void testFindAllByPost_Id() {
        Pageable pageable = PageRequest.of(0, 20);

        long start = System.currentTimeMillis();
        commentRepository.findAllByPost_Id(testPostId, pageable);
        long end = System.currentTimeMillis();

        print("findAllByPost_Id(pageable)", end - start);
    }

    @Test @Order(2)
    void testCountByPost_Id() {
        long start = System.currentTimeMillis();
        commentRepository.countByPost_Id(testPostId);
        long end = System.currentTimeMillis();

        print("countByPost_Id", end - start);
    }

    @Test @Order(3)
    void testCountByAuthor_UserId() {
        long start = System.currentTimeMillis();
        commentRepository.countByAuthor_UserId(testUserId);
        long end = System.currentTimeMillis();

        print("countByAuthor_UserId", end - start);
    }

    @Test @Order(4)
    void testDeleteAllByAuthor_UserId() {
        long start = System.currentTimeMillis();
        commentRepository.deleteAllByAuthor_UserId(testUserId);
        long end = System.currentTimeMillis();

        print("deleteAllByAuthor_UserId", end - start);
    }
}