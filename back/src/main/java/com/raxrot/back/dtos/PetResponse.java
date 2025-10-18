package com.raxrot.back.dtos;

import com.raxrot.back.enums.PetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {
    private Long id;
    private String name;
    private PetType type;
    private String breed;
    private Integer age;
    private String description;
    private String photoUrl;
    private String ownerUsername;
    private String ownerProfilePic;
}
