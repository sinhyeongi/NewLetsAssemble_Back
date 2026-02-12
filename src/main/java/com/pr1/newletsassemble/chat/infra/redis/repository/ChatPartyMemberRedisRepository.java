package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatPartyMemberRedisRepository {
    private final StringRedisTemplate redis;

    public Set<Long> getPartyMembers(long partyId){
        Set<String> s = redis.opsForSet().members(ChatRedisKeys.partyMembers(partyId));
        return validSetAndParseLong(s);
    }
    public void addPartyMember(long partyId,long userId){
        try {
            Long v = redis.opsForSet().add(ChatRedisKeys.partyMembers(partyId), String.valueOf(userId));
            if(v == null){

            }
        }catch(Exception ignored){

        }
    }
    public void removePartyMember(long partyId,long userId){
        try {
            Long v = redis.opsForSet().remove(ChatRedisKeys.partyMembers(partyId), String.valueOf(userId));
            if (v == null) {

            }
        }catch(Exception e){

        }
    }
    private Set<Long> validSetAndParseLong(Set<String> s){
        if(s == null || s.isEmpty()){return Set.of();}
        Set<Long> out = new HashSet<>();
        for(String v : s){
            try{
                out.add(Long.parseLong(v));
            }catch(Exception ignore){

            }
        }
        return out;
    }
}
