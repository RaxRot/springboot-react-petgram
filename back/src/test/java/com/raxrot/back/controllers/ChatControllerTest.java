package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.MessageResponse;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private ChatService chat;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MessageResponse messageResponse;
    private SendMessageRequest sendRequest;
    private DialogDto dialogDto;

    @BeforeEach
    void setUp() {
        messageResponse = new MessageResponse(1L, 2L, 3L, "Hello!", LocalDateTime.now(), true);
        sendRequest = new SendMessageRequest("Hey there!");
        dialogDto = new DialogDto(5L, "bob", "Hi!", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/chat/{peerId}/messages — should return conversation page")
    void getConversation_ShouldReturnPage() throws Exception {
        Page<MessageResponse> page = new PageImpl<>(List.of(messageResponse));
        Mockito.when(chat.getConversation(anyLong(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/chat/{peerId}/messages", 3L)
                        .param("page", "0")
                        .param("size", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("Hello!"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(chat, times(1)).getConversation(3L, 0, 50);
    }

    @Test
    @DisplayName("GET /api/chat/{peerId}/new — should return list of new messages")
    void getNew_ShouldReturnMessages() throws Exception {
        Mockito.when(chat.getNew(anyLong(), any())).thenReturn(List.of(messageResponse));

        mockMvc.perform(get("/api/chat/{peerId}/new", 3L)
                        .param("afterId", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Hello!"));

        verify(chat, times(1)).getNew(3L, 10L);
    }

    @Test
    @DisplayName("POST /api/chat/{peerId}/messages — should send message")
    void send_ShouldReturnCreated() throws Exception {
        Mockito.when(chat.send(anyLong(), any(SendMessageRequest.class))).thenReturn(messageResponse);

        mockMvc.perform(post("/api/chat/{peerId}/messages", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Hello!"));

        verify(chat, times(1)).send(eq(3L), any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/chat/{peerId}/read — should mark messages as read")
    void read_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/chat/{peerId}/read", 3L))
                .andExpect(status().isNoContent());

        verify(chat, times(1)).markRead(3L);
    }

    @Test
    @DisplayName("GET /api/chat/dialogs — should return user dialogs")
    void dialogs_ShouldReturnDialogs() throws Exception {
        Mockito.when(chat.getMyDialogs()).thenReturn(List.of(dialogDto));

        mockMvc.perform(get("/api/chat/dialogs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].peerUsername").value("bob"));

        verify(chat, times(1)).getMyDialogs();
    }
}
