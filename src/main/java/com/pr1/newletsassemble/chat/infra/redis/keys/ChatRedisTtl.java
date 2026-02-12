package com.pr1.newletsassemble.chat.infra.redis.keys;

import java.time.Duration;

public final class ChatRedisTtl {
    private ChatRedisTtl(){}

    // 온라인 여부
    public static final Duration PRESENCE_TTL = Duration.ofSeconds(90);

    // unread cache fresh + stale
    public static final Duration UNREAD_FRESH_TTL = Duration.ofSeconds(3);
    public static final Duration UNREAD_STALE_TTL = Duration.ofSeconds(30);
    public static final Duration UNREAD_TMP_TTL = Duration.ofSeconds(30);
    //singleflight leader lock
    public static final Duration LEADER_LOCK_TTL = Duration.ofMillis(1200);
    public static final Duration LEADER_COMPUTE_TIMEOUT = Duration.ofMillis(900);
    // dirty
    public static final Duration DIRTY_USER_PARTIES_TTL = Duration.ofMinutes(15);
    public static final Duration DIRTY_RETRY = Duration.ofMinutes(15);

    //dirty retry
    public static final Duration DIRTY_SET_TTL = Duration.ofMinutes(10);
    public static final Duration DIRTY_RETRY_TTL = Duration.ofMinutes(30);
    // dirty flush
    public static final Duration FLUSH_INTERVAL = Duration.ofSeconds(30);
    public static final Duration FLUSH_LOCK_TTL = Duration.ofSeconds(3);

    public static final Duration DIRTY_PARTIES_TTL = Duration.ofMinutes(10);
    public static final Duration DIRTY_PROCESSING_LEASE = Duration.ofSeconds(10);

}
