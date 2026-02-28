package com.pr1.newletsassemble.chat.application.port.in;

public interface GetUnreadCountUseCase {
    long getUnreadCount(Long userId);
}
