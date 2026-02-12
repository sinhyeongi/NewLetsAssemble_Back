package com.pr1.newletsassemble.chat.infra.redis.keys;

public final class ChatCacheKeys {
    private ChatCacheKeys(){}
    private static final String PRE_FIX = "chat:";
    public static String partyRecentMessages(long partyId){
        return PRE_FIX + "party:recent:" + partyId;
    }
}
