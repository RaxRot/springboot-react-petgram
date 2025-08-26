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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
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
        userRepo.findById(peerId).orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Page<Message> pg = msgRepo.findConversation(me, peerId, PageRequest.of(page, size));
        return pg.map(m -> map(m, me));
    }

    @Override
    public List<MessageResponse> getNew(Long peerId, Long afterId) {
        Long me = auth.loggedInUserId();
        userRepo.findById(peerId).orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        long aid = afterId == null ? 0L : afterId;
        return msgRepo.findNewAfter(me, peerId, aid).stream().map(m -> map(m, me)).toList();
    }

    @Transactional
    @Override
    public MessageResponse send(Long peerId, SendMessageRequest req) {
        Long meId = auth.loggedInUserId();
        if (Objects.equals(meId, peerId))
            throw new ApiException("You can't message yourself", HttpStatus.BAD_REQUEST);

        User me = userRepo.findById(meId).orElseThrow();
        User peer = userRepo.findById(peerId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Message m = new Message();
        m.setSender(me);
        m.setRecipient(peer);
        m.setText(req.getText().trim());
        Message saved = msgRepo.save(m);
        return map(saved, meId);
    }

    @Transactional
    @Override
    public void markRead(Long peerId) {
        Long me = auth.loggedInUserId();
        Page<Message> last = msgRepo.findConversation(me, peerId, PageRequest.of(0, 200));
        for (Message m : last) {
            if (!Objects.equals(m.getSender().getUserId(), me) && m.getReadAt() == null) {
                m.setReadAt(LocalDateTime.now());
            }
        }
    }

    @Override
    public List<DialogDto> getMyDialogs() {
        Long me = auth.loggedInUserId();
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
        return new ArrayList<>(map.values());
    }
}