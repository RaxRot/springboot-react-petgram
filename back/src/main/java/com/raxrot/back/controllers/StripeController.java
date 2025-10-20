package com.raxrot.back.controllers;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.services.StripeService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Stripe Payments",
        description = "Endpoints for handling Stripe checkout session creation and payment integration."
)
public class StripeController {

    private final StripeService stripeService;

    @Operation(
            summary = "Create Stripe checkout session",
            description = "Generates a Stripe checkout session for donations or purchases. Requires authentication.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Checkout session created successfully",
                            content = @Content(schema = @Schema(implementation = StripeResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid checkout request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "500", description = "Stripe API error or internal server error")
            }
    )
    @PostMapping("/checkout")
    public ResponseEntity<StripeResponse> createCheckout(
            @Parameter(description = "Checkout request containing amount, currency, and redirect URLs")
            @Valid @RequestBody CheckoutRequest req
    ) {
        log.info("💳 Stripe checkout session creation requested | amount={} | currency={}",
                req.getAmount(), req.getCurrency());
        StripeResponse resp = stripeService.createCheckoutSession(req);
        log.info("✅ Stripe checkout session created successfully | sessionId={}", resp.getSessionId());
        return ResponseEntity.ok(resp);
    }
}
