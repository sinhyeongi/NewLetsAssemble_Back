package com.pr1.newletsassemble.user.api.dto;


import java.util.List;

public record ChatPageResponse(
        List<ChatPageResponse> items,
        Long nextBeforeChatId,
        Long nextAfterChatId
) {

}
