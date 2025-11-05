package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Message;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.MessageRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("ChatServiceImpl Tests")
class ChatServiceImplTest {

    @Mock private AuthUtil authUtil;
    @Mock private UserRepository userRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User me;
    private User peer;
    private Message msg;

    @BeforeEach
    void setUp() {
        me = new User();
        me.setUserId(1L);
        me.setUserName("vlad");

        peer = new User();
        peer.setUserId(2L);
        peer.setUserName("dasha");

        msg = new Message();
        msg.setId(10L);
        msg.setSender(me);
        msg.setRecipient(peer);
        msg.setText("hi");
        msg.setCreatedAt(LocalDateTime.now());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should fetch conversation successfully")
    void should_fetch_conversation_successfully() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(2L)).willReturn(Optional.of(peer));
        given(messageRepository.findConversation(eq(1L), eq(2L), any()))
                .willReturn(new PageImpl<>(List.of(msg)));

        Page<MessageResponse> result = chatService.getConversation(2L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        MessageResponse res = result.getContent().get(0);
        assertThat(res.getText()).isEqualTo("hi");
        assertThat(res.isMine()).isTrue();

        verify(userRepository).findById(2L);
        verify(messageRepository).findConversation(eq(1L), eq(2L), any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when peer user not found (getConversation)")
    void should_throw_when_peer_not_found_conversation() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(2L, 0, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should fetch new messages after given ID")
    void should_fetch_new_messages_after_id() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(2L)).willReturn(Optional.of(peer));
        given(messageRepository.findNewAfter(1L, 2L, 5L))
                .willReturn(List.of(msg));

        List<MessageResponse> result = chatService.getNew(2L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("hi");
        verify(messageRepository).findNewAfter(1L, 2L, 5L);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when peer not found (getNew)")
    void should_throw_when_peer_not_found_getNew() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getNew(2L, 5L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when sending message to self")
    void should_throw_when_sending_to_self() {
        given(authUtil.loggedInUserId()).willReturn(1L);

        SendMessageRequest req = new SendMessageRequest("hello");

        assertThatThrownBy(() -> chatService.send(1L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("can't message yourself")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should send message successfully")
    void should_send_message_successfully() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(peer));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });

        SendMessageRequest req = new SendMessageRequest("hello");
        MessageResponse resp = chatService.send(2L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getText()).isEqualTo("hello");
        assertThat(resp.getSenderId()).isEqualTo(1L);
        assertThat(resp.getRecipientId()).isEqualTo(2L);
        assertThat(resp.isMine()).isTrue();

        verify(messageRepository).save(any(Message.class));
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should throw when recipient not found")
    void should_throw_when_recipient_not_found() {
        given(authUtil.loggedInUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        SendMessageRequest req = new SendMessageRequest("hey");

        assertThatThrownBy(() -> chatService.send(2L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should mark unread messages as read")
    void should_mark_unread_messages_as_read() {
        given(authUtil.loggedInUserId()).willReturn(1L);

        Message unread = new Message();
        unread.setSender(peer);
        unread.setRecipient(me);
        unread.setText("hello");
        unread.setCreatedAt(LocalDateTime.now());
        unread.setReadAt(null);

        Page<Message> page = new PageImpl<>(List.of(unread));
        given(messageRepository.findConversation(eq(1L), eq(2L), any())).willReturn(page);

        chatService.markRead(2L);

        assertThat(unread.getReadAt()).isNotNull();
        verify(messageRepository).findConversation(eq(1L), eq(2L), any());
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return dialogs list correctly")
    void should_return_dialogs_list_correctly() {
        given(authUtil.loggedInUserId()).willReturn(1L);

        Message m1 = new Message();
        m1.setSender(me);
        m1.setRecipient(peer);
        m1.setText("Hello Dasha");
        m1.setCreatedAt(LocalDateTime.of(2025, 10, 24, 21, 0));

        Message m2 = new Message();
        m2.setSender(peer);
        m2.setRecipient(me);
        m2.setText("Hi Vlad!");
        m2.setCreatedAt(LocalDateTime.of(2025, 10, 24, 21, 5));

        Page<Message> page = new PageImpl<>(List.of(m2, m1));
        given(messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(eq(1L), eq(1L), any()))
                .willReturn(page);

        List<DialogDto> dialogs = chatService.getMyDialogs();

        assertThat(dialogs).hasSize(1);
        DialogDto d = dialogs.get(0);
        assertThat(d.getPeerId()).isEqualTo(2L);
        assertThat(d.getPeerUsername()).isEqualTo("dasha");
        assertThat(d.getLastMessage()).isEqualTo("Hi Vlad!");

        verify(messageRepository).findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(eq(1L), eq(1L), any());
    }
}