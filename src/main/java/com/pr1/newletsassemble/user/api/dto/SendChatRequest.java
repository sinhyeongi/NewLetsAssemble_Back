package com.pr1.newletsassemble.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendChatRequest(
        @NotNull Long partyId,
        @NotBlank String clientMessageId,
        @NotBlank String content
) {
}
