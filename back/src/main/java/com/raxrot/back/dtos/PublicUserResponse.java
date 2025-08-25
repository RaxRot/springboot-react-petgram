package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserResponse {
    private Long id;
    private String userName;
    private String profilePic;
    private long followers;
    private long following;
    private boolean banned;
}
