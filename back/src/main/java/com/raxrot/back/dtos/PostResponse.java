package com.raxrot.back.dtos;

import com.raxrot.back.enums.AnimalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private AnimalType animalType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserResponseForSearch user;
}
