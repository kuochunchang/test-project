package tw.com.sc.mq.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MqServiceMetrics {
    private final Counter mqMessagesSentCounter;
    private final Timer mqProcessingTimer;
    private final Counter heartbeatFailedCounter;
    private long lastHeartbeatFailureTimestamp = 0;
    private long lastHeartbeatReceivedTimestamp = 0;
    private long requestQueueSize = 0;
    private long responseQueueSize = 0;
    private final MeterRegistry registry;
    private final Counter mqTimeouts;

    public MqServiceMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.mqMessagesSentCounter = Counter.builder("mq.messages.sent")
                .description("message sent count")
                .register(registry);

        this.mqProcessingTimer = Timer.builder("mq.processing.time")
                .description("MQ message processing time")
                .register(registry);

        this.heartbeatFailedCounter = Counter.builder("mq.heartbeat.failed")
                .description("heartbeat failed count")
                .register(registry);

        Gauge.builder("mq.heartbeat.last.failure.timestamp",
                this,
                value -> this.lastHeartbeatFailureTimestamp)
                .description("last heartbeat failure timestamp")
                .register(registry);
        Gauge.builder("mq.heartbeat.last.sent.timestamp",
                this,
                value -> this.lastHeartbeatReceivedTimestamp)
                .description("last heartbeat received timestamp")
                .register(registry);

        Gauge.builder("mq.queue.request.size",
                this,
                value -> this.requestQueueSize)
                .description("MQ request queue size")
                .register(registry);
        Gauge.builder("mq.queue.response.size",
                this,
                value -> this.responseQueueSize)
                .description("MQ response queue size")
                .register(registry);

        this.mqTimeouts = registry.counter("mq.timeouts");
    }

    public Timer.Sample startRequestTimer() {
        return Timer.start();
    }

    public void stopRequestTimer(Timer.Sample sample) {
        sample.stop(mqProcessingTimer);
    }

    public void incrementMqMessagesSent() {
        mqMessagesSentCounter.increment();
    }

    public void recordMqProcessingTime(long timeInMs) {
        mqProcessingTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public void incrementHeartbeatFailed() {
        heartbeatFailedCounter.increment();
        this.lastHeartbeatFailureTimestamp = System.currentTimeMillis();
    }

    public void recordHeartbeatReceivedTimestamp() {
        this.lastHeartbeatReceivedTimestamp = System.currentTimeMillis();
    }

    public void recordRequestQueueSize(long size) {
        this.requestQueueSize = size;
    }

    public void recordResponseQueueSize(long size) {
        this.responseQueueSize = size;
    }

    public void incrementMqTimeouts() {
        mqTimeouts.increment();
    }

}
