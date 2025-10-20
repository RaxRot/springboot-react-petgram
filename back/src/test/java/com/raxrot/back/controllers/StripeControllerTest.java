package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.services.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StripeController.class)
@AutoConfigureMockMvc(addFilters = false)
class StripeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private StripeService stripeService;
    @MockBean private com.raxrot.back.security.jwt.JwtUtils jwtUtils;
    @MockBean private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;
    @MockBean private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private CheckoutRequest checkoutRequest;
    private StripeResponse stripeResponse;

    @BeforeEach
    void setup() {
        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setAuthorId(5L);
        checkoutRequest.setAmount(1500L);
        checkoutRequest.setCurrency("usd");
        checkoutRequest.setSuccessUrl("https://petgram.com/success");
        checkoutRequest.setCancelUrl("https://petgram.com/cancel");

        stripeResponse = new StripeResponse();
        stripeResponse.setStatus("success");
        stripeResponse.setMessage("Session created");
        stripeResponse.setSessionId("sess_123456");
        stripeResponse.setSessionUrl("https://stripe.com/checkout/sess_123456");
    }

    @Test
    @DisplayName("POST /api/stripe/checkout — should create Stripe session")
    void createCheckout_ShouldReturnOk() throws Exception {
        when(stripeService.createCheckoutSession(any(CheckoutRequest.class))).thenReturn(stripeResponse);

        mockMvc.perform(post("/api/stripe/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Session created"))
                .andExpect(jsonPath("$.sessionId").value("sess_123456"))
                .andExpect(jsonPath("$.sessionUrl").value("https://stripe.com/checkout/sess_123456"));

        verify(stripeService, times(1)).createCheckoutSession(any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /api/stripe/checkout — should return 400 for invalid data")
    void createCheckout_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        checkoutRequest.setCurrency("");
        checkoutRequest.setSuccessUrl("");

        mockMvc.perform(post("/api/stripe/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest());

        verify(stripeService, times(0)).createCheckoutSession(any());
    }
}
