package com.raxrot.back.services;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;

public interface StripeService {
    StripeResponse createCheckoutSession(CheckoutRequest req);
}
