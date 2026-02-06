package com.pr1.newletsassemble.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
    @NotBlank String refreshToken
) {
}
