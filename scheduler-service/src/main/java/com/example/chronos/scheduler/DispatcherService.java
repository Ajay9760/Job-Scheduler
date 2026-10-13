package com.example.chronos.scheduler;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.messaging.JobMessage;
import com.example.chronos.messaging.RabbitMessagingConfig;
import com.example.chronos.repository.JobInstanceRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Step 2 (transactional outbox): publish PENDING instances whose time has come, wait for the broker's
 * confirm, then mark them QUEUED - all inside one DB transaction. If publishing fails the transaction
 * rolls back and the rows stay PENDING (retried next tick). If the commit fails after publishing, the
 * message is delivered twice, which is safe because the worker claims instances idempotently.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DispatcherService {

    private final JobInstanceRepository instances;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Value("${chronos.scheduler.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${chronos.scheduler.dispatch-interval-ms:500}")
    public void tick() {
        int n;
        do {
            try {
                n = dispatchBatch();
            } catch (RuntimeException e) {
                log.error("Dispatch failed, will retry: {}", e.toString());
                return;
            }
        } while (n >= batchSize);
    }

    int dispatchBatch() {
        Integer n = tx.execute(status -> {
            List<Object[]> rows = instances.lockDispatchable(clock.instant(), batchSize);
            if (rows.isEmpty()) {
                return 0;
            }
            rabbit.invoke(ops -> {
                for (Object[] r : rows) {
                    ops.convertAndSend(RabbitMessagingConfig.EXCHANGE, RabbitMessagingConfig.ROUTING_KEY,
                            new JobMessage(((Number) r[0]).longValue(), ((Number) r[1]).longValue()));
                }
                ops.waitForConfirmsOrDie(5_000);
                return null;
            });
            instances.markQueued(rows.stream().map(r -> ((Number) r[0]).longValue()).toList(), clock.instant());
            return rows.size();
        });
        return n == null ? 0 : n;
    }
}
