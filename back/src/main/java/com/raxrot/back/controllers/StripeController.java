package com.raxrot.back.controllers;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.services.StripeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;

    @PostMapping("/checkout")
    public ResponseEntity<StripeResponse> createCheckout(@Valid @RequestBody CheckoutRequest req) {
        StripeResponse resp = stripeService.createCheckoutSession(req);
        return ResponseEntity.ok(resp);
    }
}
