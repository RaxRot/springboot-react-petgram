package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {
    private Long id;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorUsername;
}

