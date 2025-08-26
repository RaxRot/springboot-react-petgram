package com.raxrot.back.controllers;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;

    @GetMapping("/{peerId}/messages")
    public ResponseEntity<Map<String,Object>> getConversation(
            @PathVariable Long peerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<MessageResponse> pg = chat.getConversation(peerId, page, size);
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
    public List<MessageResponse> getNew(@PathVariable Long peerId,
                                        @RequestParam(required = false) Long afterId) {
        return chat.getNew(peerId, afterId);
    }

    @PostMapping("/{peerId}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long peerId,
                                                @Valid @RequestBody SendMessageRequest req) {
        return new ResponseEntity<>(chat.send(peerId, req), HttpStatus.CREATED);
    }

    @PatchMapping("/{peerId}/read")
    public ResponseEntity<Void> read(@PathVariable Long peerId) {
        chat.markRead(peerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dialogs")
    public ResponseEntity<List<DialogDto>> dialogs() {
        return ResponseEntity.ok(chat.getMyDialogs());
    }
}