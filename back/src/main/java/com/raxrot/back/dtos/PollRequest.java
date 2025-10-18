package com.raxrot.back.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollRequest {
    @NotBlank
    private String question;

    @NotEmpty
    @Size(min = 2, max = 6, message = "Poll must have 2–6 options")
    private List<String> options;
}
