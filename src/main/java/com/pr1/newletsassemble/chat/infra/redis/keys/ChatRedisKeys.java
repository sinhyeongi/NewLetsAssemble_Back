package com.pr1.newletsassemble.chat.infra.redis.keys;

public final class ChatRedisKeys {
    private ChatRedisKeys(){}
    private static final String PRE_FIX = "chat:";

    // party:last_seq:{partyId} -> STRING
    public static String partyLastSeq(long partyId){
        return PRE_FIX + "party:last_seq:" + partyId;
    }

    // user:read_seq:{userId} -> ZSET member = partyId score = lastReadSeq
    public static String userReadSeq(long userId){
        return PRE_FIX + "user:read_seq:" + userId;
    }

    // party:members:{partyId} -> SET member=userId
    public static String partyMembers(long partyId){
        return PRE_FIX + "party:members:" + partyId;
    }

    // party:presence:{partyId} -> ZSET member=userId score=expireAtMillis
    public static String partyPresence(long partyId){
        return PRE_FIX + "party:presence:" + partyId;
    }

    //unread fresh/stale
    public static String unreadFresh(long userId){
        return PRE_FIX + "unread:fresh:" + userId;
    }
    public static String unreadStale(long userId){
        return PRE_FIX + "unread:stale:" + userId;
    }
    // write-then-swap tmp keys(nonce 필요)
    public static String unreadFreshTmp(long userId,String nonce){
        return PRE_FIX + "unread:fresh:tmp:" + userId + ":" + nonce;
    }
    public static String unreadStaleTmp(long userId,String nonce){
        return PRE_FIX + "unread:fresh:tmp:" + userId + ":" + nonce;
    }
    public static String unreadNonce(long userId){
        return PRE_FIX + "unread:nonce:" + userId;
    }
    //leader lock
    public static String unreadLeaderLock(Long userId){
        return PRE_FIX + "lock:unread_leader:" + userId;
    }

    // dirty tracking
    public static String dirtyUsers(){
        return PRE_FIX + "dirty:users";
    }
    public static String dirtyProcessing(){
        return PRE_FIX + "dirty:processing";
    }
    public static String dirtyUserParties(long userId){
        return PRE_FIX + "dirty:user:" + userId;
    }
    public static String dirtyRetry(long userId){
        return PRE_FIX + "dirty:retry:" + userId;
    }

    // flush lock per user
    public static String flushLock(long userId){
        return PRE_FIX + "lock:flush:" + userId;
    }


}
