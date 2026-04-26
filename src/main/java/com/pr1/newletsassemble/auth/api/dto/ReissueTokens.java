package com.pr1.newletsassemble.auth.api.dto;

public record ReissueTokens(
        String access,
        String newRefresh
) {
}
