package com.raxrot.back.controllers;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chat;

    @GetMapping("/{peerId}/messages")
    public ResponseEntity<Map<String, Object>> getConversation(
            @PathVariable Long peerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        log.info("💬 Fetching conversation with peerId={} (page={}, size={})", peerId, page, size);
        Page<MessageResponse> pg = chat.getConversation(peerId, page, size);
        log.info("✅ Retrieved {} messages from conversation with peerId={}", pg.getContent().size(), peerId);
        return ResponseEntity.ok(Map.of(
                "content", pg.getContent(),
                "pageNumber", pg.getNumber(),
                "pageSize", pg.getSize(),
                "totalElements", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "lastPage", pg.isLast()
        ));
    }

    @GetMapping("/{peerId}/new")
    public List<MessageResponse> getNew(
            @PathVariable Long peerId,
            @RequestParam(required = false) Long afterId
    ) {
        log.info("📨 Fetching new messages with peerId={} after messageId={}", peerId, afterId);
        List<MessageResponse> newMessages = chat.getNew(peerId, afterId);
        log.info("✅ Found {} new messages with peerId={}", newMessages.size(), peerId);
        return newMessages;
    }

    @PostMapping("/{peerId}/messages")
    public ResponseEntity<MessageResponse> send(
            @PathVariable Long peerId,
            @Valid @RequestBody SendMessageRequest req
    ) {
        String preview = req.getText() != null && req.getText().length() > 30
                ? req.getText().substring(0, 30) + "..."
                : req.getText();

        log.info("✉️ Sending message to peerId={} | text preview='{}'", peerId, preview);
        MessageResponse sent = chat.send(peerId, req);
        log.info("✅ Message sent successfully to peerId={} with messageId={}", peerId, sent.getId());
        return new ResponseEntity<>(sent, HttpStatus.CREATED);
    }

    @PatchMapping("/{peerId}/read")
    public ResponseEntity<Void> read(@PathVariable Long peerId) {
        log.info("👀 Marking messages as read for peerId={}", peerId);
        chat.markRead(peerId);
        log.info("✅ Messages marked as read for peerId={}", peerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dialogs")
    public ResponseEntity<List<DialogDto>> dialogs() {
        log.info("📁 Fetching dialogs for current user");
        List<DialogDto> dialogs = chat.getMyDialogs();
        log.info("✅ Retrieved {} dialogs", dialogs.size());
        return ResponseEntity.ok(dialogs);
    }
}
