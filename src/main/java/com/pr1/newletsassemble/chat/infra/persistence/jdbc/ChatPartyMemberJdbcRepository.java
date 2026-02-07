package com.pr1.newletsassemble.chat.infra.persistence.jdbc;

import com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto.BatchSummary;
import com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatPartyMemberJdbcRepository {
    private final JdbcTemplate jdbc;

    public BatchSummary batchUpdateLastRead(List<Row>rows , Instant now){
        if(rows == null|| rows.isEmpty()) return new BatchSummary(0,0,0,0);

        String sql = """
                UPDATE party_member
                SET
                    last_read_seq = GREATEST(last_read_seq,?),
                    last_read_at = CASE
                                        WHEN ? > last_read_seq THEN ?
                                        ELSE last_read_at
                                   end
                    WHERE party_id = ?
                    AND user_id = ?
                """;
        int [][] result = jdbc.batchUpdate(sql,rows,500,(PreparedStatement ps , Row r) ->{
            ps.setLong(1,r.lastReadSeq());
            ps.setLong(2,r.lastReadSeq());
            ps.setObject(3,now);
            ps.setLong(4,r.partyId());
            ps.setLong(5,r.userId());
        });
        int total=0,updated=0,noInfo=0,failed=0;
        for(int[] r : result){
            for(int x : r){
                total++;
                if(x > 0) updated += x;
                else if(x == Statement.SUCCESS_NO_INFO) noInfo++;
                else if( x == Statement.EXECUTE_FAILED) failed++;
            }
        }
        return new BatchSummary(total,updated,noInfo,failed);
    }
}
