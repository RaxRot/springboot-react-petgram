package com.raxrot.back.controllers;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.services.StripeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeController {

    private final StripeService stripeService;

    @PostMapping("/checkout")
    public ResponseEntity<StripeResponse> createCheckout(@Valid @RequestBody CheckoutRequest req) {
        log.info("💳 Stripe checkout session creation requested | amount={} | currency={}",
                req.getAmount(), req.getCurrency());
        StripeResponse resp = stripeService.createCheckoutSession(req);
        log.info("✅ Stripe checkout session created successfully | sessionId={}", resp.getSessionId());
        return ResponseEntity.ok(resp);
    }
}
