package com.substring.docmind.dto;

import com.substring.docmind.entity.Role;

public record RegisterUserRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 3, max = 50) String username,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 254) String email,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 8, max = 72) String password
) {
}
