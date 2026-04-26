package com.pr1.newletsassemble.party.infra.redis.key;

public final class PartyMemberRedisKeys {
    private PartyMemberRedisKeys(){}
    private static final String PRE_FIX = "party:";

    // party:members:{partyId} -> SET member=userId
    public static String partyMembers(long partyId){
        return PRE_FIX + "members:" + partyId;
    }
}
