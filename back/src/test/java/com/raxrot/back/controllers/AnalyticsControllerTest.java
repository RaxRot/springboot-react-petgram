package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.services.AnalyticsService;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ✅ Unit-тест для AnalyticsController.
 * Без Security/JWT, полностью замоканные зависимости.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;


    @MockBean
    private AnalyticsService analyticsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserStatsResponse userStatsResponse;

    @BeforeEach
    void setUp() {
        userStatsResponse = new UserStatsResponse(
                5,   // totalPosts
                100, // totalLikes
                25,  // totalComments
                300, // totalViews
                2,   // totalPets
                50,  // totalFollowers
                40,  // totalFollowing
                10   // totalDonationsReceived
        );
    }

    @Test
    @DisplayName("GET /api/user/stats — should return user statistics")
    void getMyStats_ShouldReturnUserStatsResponse() throws Exception {
        Mockito.when(analyticsService.getMyStats()).thenReturn(userStatsResponse);

        mockMvc.perform(get("/api/user/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPosts").value(5))
                .andExpect(jsonPath("$.totalLikes").value(100))
                .andExpect(jsonPath("$.totalComments").value(25))
                .andExpect(jsonPath("$.totalViews").value(300))
                .andExpect(jsonPath("$.totalPets").value(2))
                .andExpect(jsonPath("$.totalFollowers").value(50))
                .andExpect(jsonPath("$.totalFollowing").value(40))
                .andExpect(jsonPath("$.totalDonationsReceived").value(10));

        verify(analyticsService, times(1)).getMyStats();
    }
}
