package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {
    private Long pollId;
    private String question;
    private List<PollOptionResponse> options;
    private boolean voted;
}

