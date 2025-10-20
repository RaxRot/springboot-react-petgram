package com.raxrot.back.controllers;

import com.raxrot.back.dtos.PollOptionResponse;
import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.dtos.PollResponse;
import com.raxrot.back.services.PollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@WebMvcTest(controllers = PollController.class)
@AutoConfigureMockMvc(addFilters = false)
class PollControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PollService pollService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private PollRequest pollRequest;
    private PollResponse pollResponse;

    @BeforeEach
    void setup() {
        pollRequest = new PollRequest(
                "Which pet is cuter?",
                List.of("Cat", "Dog")
        );

        pollResponse = new PollResponse(
                1L,
                "Which pet is cuter?",
                List.of(
                        new PollOptionResponse(1L, "Cat", 5),
                        new PollOptionResponse(2L, "Dog", 7)
                ),
                false
        );
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/polls — should create poll")
    void createPoll_ShouldReturnCreatedPoll() throws Exception {
        Mockito.when(pollService.createPoll(anyLong(), any(PollRequest.class))).thenReturn(pollResponse);

        mockMvc.perform(post("/api/posts/{postId}/polls", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pollRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pollId").value(1L))
                .andExpect(jsonPath("$.question").value("Which pet is cuter?"))
                .andExpect(jsonPath("$.options[0].optionText").value("Cat"))
                .andExpect(jsonPath("$.options[1].optionText").value("Dog"));


        verify(pollService, times(1)).createPoll(eq(10L), any(PollRequest.class));
    }


    @Test
    @DisplayName("GET /api/posts/{postId}/polls — should return poll")
    void getPoll_ShouldReturnPoll() throws Exception {
        Mockito.when(pollService.getPoll(anyLong())).thenReturn(pollResponse);

        mockMvc.perform(get("/api/posts/{postId}/polls", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollId").value(1L))
                .andExpect(jsonPath("$.question").value("Which pet is cuter?"))
                .andExpect(jsonPath("$.options[1].votes").value(7));

        verify(pollService, times(1)).getPoll(10L);
    }

    @Test
    @DisplayName("POST /api/polls/{pollId}/vote/{optionId} — should register vote")
    void vote_ShouldReturnUpdatedPoll() throws Exception {
        PollResponse votedResponse = new PollResponse(
                1L,
                "Which pet is cuter?",
                List.of(
                        new PollOptionResponse(1L, "Cat", 6),
                        new PollOptionResponse(2L, "Dog", 7)
                ),
                true
        );

        Mockito.when(pollService.vote(anyLong(), anyLong())).thenReturn(votedResponse);

        mockMvc.perform(post("/api/polls/{pollId}/vote/{optionId}", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voted").value(true))
                .andExpect(jsonPath("$.options[0].votes").value(6));

        verify(pollService, times(1)).vote(1L, 1L);
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/polls — should delete poll")
    void deletePoll_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/polls", 10L))
                .andExpect(status().isNoContent());

        verify(pollService, times(1)).deletePoll(10L);
    }
}
