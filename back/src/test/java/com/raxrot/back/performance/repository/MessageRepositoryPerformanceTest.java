package com.raxrot.back.performance.repository;

import com.raxrot.back.models.Message;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.MessageRepository;
import com.raxrot.back.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("🔬 MessageRepository Performance Tests")
class MessageRepositoryPerformanceTest {

    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private static final int USERS = 5_000;
    private static final int MESSAGES = 50_000;

    private Long meId;
    private Long peerId;

    @BeforeEach
    void setUp() {
        if (messageRepository.count() > 0) return;

        List<User> users = new ArrayList<>(USERS);
        for (int i = 1; i <= USERS; i++) {
            users.add(new User("u" + i, "u" + i + "@mail.com", "pwd"));
        }
        userRepository.saveAll(users);
        em.flush(); em.clear();

        meId = users.get(0).getUserId();
        peerId = users.get(1).getUserId();

        List<Message> msgs = new ArrayList<>(MESSAGES);
        for (int i = 0; i < MESSAGES; i++) {
            User sender = (i % 2 == 0) ? users.get(0) : users.get(1);
            User rec = (i % 2 == 0) ? users.get(1) : users.get(0);

            Message m = new Message();
            m.setSender(sender);
            m.setRecipient(rec);
            m.setText("msg" + i);
            m.setCreatedAt(LocalDateTime.now().minusMinutes(MESSAGES - i));
            msgs.add(m);

            if (msgs.size() % 1000 == 0) {
                messageRepository.saveAll(msgs);
                msgs.clear();
                em.flush(); em.clear();
            }
        }

        if (!msgs.isEmpty()) {
            messageRepository.saveAll(msgs);
            em.flush(); em.clear();
        }
    }

    @Test
    @DisplayName("⏱ findConversation — performance")
    void findConversation_performance() {
        Pageable p = PageRequest.of(0, 20);

        StopWatch sw = new StopWatch("MessageRepository.findConversation");

        sw.start("page 1");
        messageRepository.findConversation(meId, peerId, p);
        sw.stop();

        sw.start("page 2");
        messageRepository.findConversation(meId, peerId, PageRequest.of(1, 20));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findConversation ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @DisplayName("⏱ findNewAfter — performance")
    void findNewAfter_performance() {
        Long afterId = 100L;

        StopWatch sw = new StopWatch("MessageRepository.findNewAfter");

        sw.start("find new");
        messageRepository.findNewAfter(meId, peerId, afterId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findNewAfter ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }


    @Test
    @DisplayName("⏱ find inbox/outbox — performance")
    void findInboxOutbox_performance() {
        StopWatch sw = new StopWatch("MessageRepository.findBySenderOrRecipient");

        Pageable p = PageRequest.of(0, 20);

        sw.start("page 1");
        messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(meId, meId, p);
        sw.stop();

        sw.start("page 2");
        messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(meId, meId, PageRequest.of(1, 20));
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }

    @Test
    @Transactional
    @DisplayName("⏱ deleteAllForUser — performance")
    void deleteAllForUser_performance() {
        StopWatch sw = new StopWatch("MessageRepository.deleteAllForUser");

        sw.start("delete for user");
        messageRepository.deleteAllForUser(meId);
        sw.stop();

        System.out.println("\n========== 📊 PERFORMANCE REPORT: deleteAllForUser ==========");
        for (var t : sw.getTaskInfo())
            System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
        System.out.println("============================================================\n");
    }
}