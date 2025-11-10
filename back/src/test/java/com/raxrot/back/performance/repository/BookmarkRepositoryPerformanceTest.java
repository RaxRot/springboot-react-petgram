package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Bookmark;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.BookmarkRepository;
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
@DisplayName("🔖 BookmarkRepository Performance Tests")
class BookmarkRepositoryPerformanceTest {

    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;

    private Long testUserId;
    private Long testPostId;

    @BeforeEach
    void setUp() {
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();


        User u = new User("bm_user", Math.random() + "@mail.com", "pwd");
        userRepository.save(u);
        testUserId = u.getUserId();


        List<Bookmark> list = new ArrayList<>();

        for (int i = 0; i < 2000; i++) {
            Post p = new Post();
            p.setTitle("Title " + i);
            p.setContent("Content " + i);
            p.setUser(u);
            postRepository.save(p);

            if (i == 0) testPostId = p.getId();

            Bookmark b = new Bookmark();
            b.setUser(u);
            b.setPost(p);
            list.add(b);
        }

        bookmarkRepository.saveAll(list);

        // Warm-up
        bookmarkRepository.existsByPost_IdAndUser_UserId(testPostId, testUserId);
    }

    private void print(String label, long ms) {
        System.out.println("========== 📊 " + label + " ==========");
        System.out.println(label + " → " + ms + " ms");
    }

    @Test @Order(1)
    void testExistsByPost_IdAndUser_UserId() {
        long start = System.currentTimeMillis();
        bookmarkRepository.existsByPost_IdAndUser_UserId(testPostId, testUserId);
        long end = System.currentTimeMillis();
        print("existsByPost_IdAndUser_UserId", end - start);
    }

    @Test @Order(2)
    void testFindAllByUser_UserId() {
        Pageable pageable = PageRequest.of(0, 20);

        long start = System.currentTimeMillis();
        bookmarkRepository.findAllByUser_UserId(testUserId, pageable);
        long end = System.currentTimeMillis();
        print("findAllByUser_UserId(pageable)", end - start);
    }

    @Test @Order(3)
    void testDeleteByPost_IdAndUser_UserId() {
        long start = System.currentTimeMillis();
        bookmarkRepository.deleteByPost_IdAndUser_UserId(testPostId, testUserId);
        long end = System.currentTimeMillis();
        print("deleteByPost_IdAndUser_UserId", end - start);
    }

    @Test @Order(4)
    void testDeleteAllByUser_UserId() {
        long start = System.currentTimeMillis();
        bookmarkRepository.deleteAllByUser_UserId(testUserId);
        long end = System.currentTimeMillis();
        print("deleteAllByUser_UserId", end - start);
    }
}