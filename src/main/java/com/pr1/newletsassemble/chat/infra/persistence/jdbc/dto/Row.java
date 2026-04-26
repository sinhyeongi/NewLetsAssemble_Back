package com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto;

public record Row(long userId,long partyId,long lastReadSeq) {
}
