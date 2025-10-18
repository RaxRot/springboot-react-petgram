package com.raxrot.back.dtos;

import com.raxrot.back.enums.PetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetRequest {

    @NotBlank(message = "Pet name is required")
    @Size(max = 50)
    private String name;

    private PetType type = PetType.OTHER;

    private String breed;
    private Integer age;

    @Size(max = 500)
    private String description;
}
