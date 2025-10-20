package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Message;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.MessageRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.ChatService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final AuthUtil auth;
    private final UserRepository userRepo;
    private final MessageRepository msgRepo;

    private MessageResponse map(Message m, Long me) {
        return new MessageResponse(
                m.getId(),
                m.getSender().getUserId(),
                m.getRecipient().getUserId(),
                m.getText(),
                m.getCreatedAt(),
                Objects.equals(m.getSender().getUserId(), me)
        );
    }

    @Override
    public Page<MessageResponse> getConversation(Long peerId, int page, int size) {
        Long me = auth.loggedInUserId();
        log.info("User {} is fetching conversation with peer {} (page={}, size={})", me, peerId, page, size);

        userRepo.findById(peerId)
                .orElseThrow(() -> {
                    log.error("User {} attempted to fetch conversation with non-existing user {}", me, peerId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        Page<Message> pg = msgRepo.findConversation(me, peerId, PageRequest.of(page, size));
        log.info("Conversation loaded: {} messages found between {} and {}", pg.getTotalElements(), me, peerId);
        return pg.map(m -> map(m, me));
    }

    @Override
    public List<MessageResponse> getNew(Long peerId, Long afterId) {
        Long me = auth.loggedInUserId();
        long aid = afterId == null ? 0L : afterId;
        log.info("User {} is fetching new messages with peer {} after message ID {}", me, peerId, aid);

        userRepo.findById(peerId)
                .orElseThrow(() -> {
                    log.error("User {} attempted to fetch new messages from non-existing user {}", me, peerId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        List<MessageResponse> newMessages = msgRepo.findNewAfter(me, peerId, aid).stream()
                .map(m -> map(m, me))
                .toList();

        log.info("User {} received {} new messages from peer {}", me, newMessages.size(), peerId);
        return newMessages;
    }

    @Transactional
    @Override
    public MessageResponse send(Long peerId, SendMessageRequest req) {
        Long meId = auth.loggedInUserId();
        log.info("User {} attempting to send message to peer {}", meId, peerId);

        if (Objects.equals(meId, peerId)) {
            log.warn("User {} attempted to message themselves", meId);
            throw new ApiException("You can't message yourself", HttpStatus.BAD_REQUEST);
        }

        User me = userRepo.findById(meId)
                .orElseThrow(() -> {
                    log.error("User {} not found during message send", meId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        User peer = userRepo.findById(peerId)
                .orElseThrow(() -> {
                    log.error("Recipient {} not found for sender {}", peerId, meId);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND);
                });

        Message m = new Message();
        m.setSender(me);
        m.setRecipient(peer);
        m.setText(req.getText().trim());
        Message saved = msgRepo.save(m);

        log.info("Message sent from {} to {} (messageId={})", meId, peerId, saved.getId());
        return map(saved, meId);
    }

    @Transactional
    @Override
    public void markRead(Long peerId) {
        Long me = auth.loggedInUserId();
        log.info("User {} is marking messages as read with peer {}", me, peerId);

        Page<Message> last = msgRepo.findConversation(me, peerId, PageRequest.of(0, 200));
        int count = 0;

        for (Message m : last) {
            if (!Objects.equals(m.getSender().getUserId(), me) && m.getReadAt() == null) {
                m.setReadAt(LocalDateTime.now());
                count++;
            }
        }

        log.info("User {} marked {} messages as read from peer {}", me, count, peerId);
    }

    @Override
    public List<DialogDto> getMyDialogs() {
        Long me = auth.loggedInUserId();
        log.info("User {} fetching their dialogs list", me);

        Page<Message> page = msgRepo.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(
                me, me, PageRequest.of(0, 300));

        Map<Long, DialogDto> map = new LinkedHashMap<>();

        for (Message m : page.getContent()) {
            Long peerId = m.getSender().getUserId().equals(me)
                    ? m.getRecipient().getUserId()
                    : m.getSender().getUserId();

            if (!map.containsKey(peerId)) {
                String username = m.getSender().getUserId().equals(me)
                        ? m.getRecipient().getUserName()
                        : m.getSender().getUserName();

                map.put(peerId, new DialogDto(
                        peerId,
                        username,
                        m.getText(),
                        m.getCreatedAt()
                ));
            }
        }

        log.info("User {} has {} active dialogs", me, map.size());
        return new ArrayList<>(map.values());
    }
}
