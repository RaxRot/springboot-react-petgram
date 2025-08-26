package com.raxrot.back.services;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ChatService {
    Page<MessageResponse> getConversation(Long peerId, int page, int size);
    List<MessageResponse> getNew(Long peerId, Long afterId);
    MessageResponse send(Long peerId, SendMessageRequest req);
    void markRead(Long peerId);
    List<DialogDto> getMyDialogs();
}
