package com.raxrot.back.repositories;

import com.raxrot.back.models.Message;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("alice", "alice@example.com", "pwd"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "pwd"));
        user3 = userRepository.save(new User("charlie", "charlie@example.com", "pwd"));

        // Fixed base time
        LocalDateTime base = LocalDateTime.of(2025, 10, 22, 15, 0);

        // Stable, predictable timestamps (ordered by time)
        messageRepository.save(new Message(null, user1, user2, "Hello Bob", base.minusMinutes(10), null)); // oldest
        messageRepository.save(new Message(null, user2, user1, "Hi Alice", base.minusMinutes(5), null));
        messageRepository.save(new Message(null, user3, user1, "Hey Alice", base.minusMinutes(3), null));
        messageRepository.save(new Message(null, user1, user2, "How are you?", base.minusMinutes(1), null)); // newest
    }




    @Test
    @DisplayName("findConversation should return all messages between two users in ascending order")
    void findConversation_ReturnsMessagesBetweenTwoUsers() {
        Page<Message> conversation = messageRepository.findConversation(user1.getUserId(), user2.getUserId(), PageRequest.of(0, 10));

        assertThat(conversation.getTotalElements()).isEqualTo(3);
        assertThat(conversation.getContent().get(0).getText()).isEqualTo("Hello Bob");
        assertThat(conversation.getContent().get(1).getText()).isEqualTo("Hi Alice");
        assertThat(conversation.getContent().get(2).getText()).isEqualTo("How are you?");
    }

    @Test
    @DisplayName("findNewAfter should return messages after a given message id")
    void findNewAfter_ReturnsMessagesAfterId() {
        List<Message> allMessages = messageRepository.findConversation(user1.getUserId(), user2.getUserId(), PageRequest.of(0, 10)).getContent();
        Long afterId = allMessages.get(0).getId();

        List<Message> newMessages = messageRepository.findNewAfter(user1.getUserId(), user2.getUserId(), afterId);

        assertThat(newMessages).hasSize(2);
        assertThat(newMessages.get(0).getText()).isEqualTo("Hi Alice");
        assertThat(newMessages.get(1).getText()).isEqualTo("How are you?");
    }

    @Test
    @DisplayName("findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc should return all user messages in descending order")
    void findBySenderOrRecipient_ReturnsAllUserMessages() {
        Page<Message> messages = messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(user1.getUserId(), user1.getUserId(), PageRequest.of(0, 10));

        assertThat(messages.getTotalElements()).isEqualTo(4);
        assertThat(messages.getContent().get(0).getText()).isEqualTo("How are you?");
        assertThat(messages.getContent().get(1).getText()).isEqualTo("Hey Alice");
        assertThat(messages.getContent().get(2).getText()).isEqualTo("Hi Alice");
        assertThat(messages.getContent().get(3).getText()).isEqualTo("Hello Bob");
    }

    @Test
    @DisplayName("deleteAllForUser should remove all messages sent or received by a user")
    void deleteAllForUser_RemovesAllMessagesForUser() {
        messageRepository.deleteAllForUser(user3.getUserId());

        List<Message> remaining = messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(user3.getUserId(), user3.getUserId(), PageRequest.of(0, 10)).getContent();
        assertThat(remaining).isEmpty();

        // Ensure messages between user1 and user2 remain
        Page<Message> user1User2 = messageRepository.findConversation(user1.getUserId(), user2.getUserId(), PageRequest.of(0, 10));
        assertThat(user1User2.getTotalElements()).isEqualTo(3);
    }
}
