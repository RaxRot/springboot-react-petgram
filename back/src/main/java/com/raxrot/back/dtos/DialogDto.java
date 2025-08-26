package com.raxrot.back.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DialogDto {
    private Long peerId;
    private String peerUsername;
    private String lastMessage;
    private LocalDateTime lastAt;
}
