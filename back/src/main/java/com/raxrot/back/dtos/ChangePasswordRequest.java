package com.raxrot.back.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "Current password cannot be blank")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 5, max = 50, message = "New password must be between 5 and 50 characters")
    private String newPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 5, max = 50, message = "New password must be between 5 and 50 characters")
    private String confirmPassword;
}
