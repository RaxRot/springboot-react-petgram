package com.raxrot.back.controllers;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Chat",
        description = "Endpoints for user-to-user chat: messaging, dialogs, and message management."
)
@SecurityRequirement(name = "Bearer Authentication")
public class ChatController {

    private final ChatService chat;

    @Operation(
            summary = "Get conversation with a user",
            description = "Returns a paginated list of messages between the current user and the specified peer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Conversation fetched successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Peer not found")
            }
    )
    @GetMapping("/{peerId}/messages")
    public ResponseEntity<Map<String, Object>> getConversation(
            @Parameter(description = "ID of the chat partner") @PathVariable Long peerId,
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 50)") @RequestParam(defaultValue = "50") int size
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

    @Operation(
            summary = "Get new messages",
            description = "Fetches all messages sent after a specific message ID (for real-time updates).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "New messages fetched successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/{peerId}/new")
    public List<MessageResponse> getNew(
            @Parameter(description = "ID of the chat partner") @PathVariable Long peerId,
            @Parameter(description = "Optional: last seen message ID") @RequestParam(required = false) Long afterId
    ) {
        log.info("📨 Fetching new messages with peerId={} after messageId={}", peerId, afterId);
        List<MessageResponse> newMessages = chat.getNew(peerId, afterId);
        log.info("✅ Found {} new messages with peerId={}", newMessages.size(), peerId);
        return newMessages;
    }

    @Operation(
            summary = "Send a message",
            description = "Sends a new text message to the specified peer.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Message sent successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PostMapping("/{peerId}/messages")
    public ResponseEntity<MessageResponse> send(
            @Parameter(description = "ID of the recipient user") @PathVariable Long peerId,
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

    @Operation(
            summary = "Mark messages as read",
            description = "Marks all messages from the specified peer as read.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Messages marked as read"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PatchMapping("/{peerId}/read")
    public ResponseEntity<Void> read(
            @Parameter(description = "ID of the peer whose messages are being marked as read") @PathVariable Long peerId
    ) {
        log.info("👀 Marking messages as read for peerId={}", peerId);
        chat.markRead(peerId);
        log.info("✅ Messages marked as read for peerId={}", peerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get user dialogs",
            description = "Returns a list of all dialog threads (active chats) of the current user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dialogs retrieved successfully",
                            content = @Content(schema = @Schema(implementation = DialogDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/dialogs")
    public ResponseEntity<List<DialogDto>> dialogs() {
        log.info("📁 Fetching dialogs for current user");
        List<DialogDto> dialogs = chat.getMyDialogs();
        log.info("✅ Retrieved {} dialogs", dialogs.size());
        return ResponseEntity.ok(dialogs);
    }
}
