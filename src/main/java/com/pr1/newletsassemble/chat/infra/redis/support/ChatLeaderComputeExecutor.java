package com.pr1.newletsassemble.chat.infra.redis.support;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Component
public class ChatLeaderComputeExecutor {

    private final ExecutorService pool =
            Executors.newFixedThreadPool(4, r ->{
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("chat-unread-leader");
                return t;
            });
    @PreDestroy
    public void shutdown(){
        pool.shutdownNow();
    }

    public Optional<Map<Long,Long>> computeWithTimeout(Callable<Map<Long,Long>> job){
        Future<Map<Long,Long>> f = pool.submit(job);
        try{
            return Optional.ofNullable(
                    f.get(ChatRedisTtl.LEADER_COMPUTE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            );
        }catch(TimeoutException te){
            f.cancel(true);
            return Optional.empty();
        }catch(Exception e){
            f.cancel(true);
            return Optional.empty();
        }

    }
}
