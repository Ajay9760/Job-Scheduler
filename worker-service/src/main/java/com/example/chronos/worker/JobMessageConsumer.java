package com.example.chronos.worker;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.messaging.JobMessage;
import com.example.chronos.messaging.RabbitMessagingConfig;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.service.JobLifecycleService;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent consumer. Delivery is at-least-once, so the FIRST thing we do is an atomic conditional
 * UPDATE (PENDING|QUEUED -> RUNNING). Only the winner executes; duplicates and redeliveries are no-ops.
 * Business failures never throw (that would just requeue); they are recorded and retried via the DB.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobMessageConsumer {

    private final JobLifecycleService lifecycle;
    private final JobInstanceRepository instances;
    private final HttpJobExecutor executor;
    private final HostCircuitBreaker breaker;
    private final WebhookNotifier webhook;
    private final Clock clock;
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    @RabbitListener(queues = RabbitMessagingConfig.QUEUE)
    public void onMessage(JobMessage msg) {
        if (!lifecycle.claim(msg.instanceId(), workerId)) {
            log.debug("Instance {} already claimed or finished; skipping", msg.instanceId());
            return;
        }
        JobInstance inst = instances.findWithJobById(msg.instanceId()).orElse(null);
        if (inst == null) {
            return;
        }
        Job job = inst.getJob();
        try {
            String host = hostOf(job.getTargetUrl());
            var until = breaker.blockedUntil(host);
            if (until != null) {
                lifecycle.defer(inst.getId(), until, "circuit open for " + host);
                return;
            }
            var r = executor.execute(job);
            if (r.success()) {
                breaker.success(host);
            } else {
                breaker.failure(host);
            }
            InstanceStatus to = r.success() ? InstanceStatus.SUCCESS : r.timeout() ? InstanceStatus.TIMEOUT : InstanceStatus.FAILED;
            lifecycle.finish(inst.getId(), to, r.durationMs(), r.status(), r.body(), r.error());
            webhook.notify(job, inst.getId(), to, r.status(), r.error());
        } catch (RuntimeException e) {
            log.error("Unexpected error running instance {}", inst.getId(), e);
            lifecycle.finish(inst.getId(), InstanceStatus.FAILED, null, null, null, "Internal error: " + e);
        }
    }

    private static String hostOf(String url) {
        String h = URI.create(url).getHost();
        return h == null ? url : h.toLowerCase();
    }
}
