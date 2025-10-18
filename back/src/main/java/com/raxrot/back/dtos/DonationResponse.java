package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DonationResponse {
    private Long id;
    private String donorUsername;
    private String receiverUsername;
    private Long amount;
    private String currency;
    private LocalDateTime createdAt;
}

