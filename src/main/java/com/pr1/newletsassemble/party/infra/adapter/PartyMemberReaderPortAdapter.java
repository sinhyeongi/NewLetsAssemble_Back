package com.pr1.newletsassemble.party.infra.adapter;

import com.pr1.newletsassemble.chat.application.port.out.PartyMemberReaderPort;
import com.pr1.newletsassemble.party.infra.persistence.jpa.PartyMemberJpaRepository;
import com.pr1.newletsassemble.party.infra.redis.key.PartyMemberRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartyMemberReaderPortAdapter implements PartyMemberReaderPort {
    private final StringRedisTemplate redis;
    private final PartyMemberJpaRepository jpa;
    @Override
    public boolean exists(Long roomId, Long userId) {
        String key = PartyMemberRedisKeys.partyMembers(roomId);
        Boolean b = redis.opsForSet().isMember(key,String.valueOf(userId));
        if(Boolean.TRUE.equals(b)){
            return true;
        }
        boolean exists = jpa.existsByParty_IdAndUser_Id(roomId,userId);
        if(exists){
            redis.opsForSet().add(key,String.valueOf(userId));
        }
        return exists;
    }
}
