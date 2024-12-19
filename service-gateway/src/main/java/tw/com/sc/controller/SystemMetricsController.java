package tw.com.sc.controller;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.sc.model.Metrics;

/**
 * 不直接使用 Spring Boot 的 Actuator endpoint， 在這裡整合需要的指標，
 */
@RestController
public class SystemMetricsController {

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsController.class);

    private final MetricsEndpoint metricsEndpoint;
    private final List<String> metricsNames = Arrays.asList("api.requests.total", "api.request.latency",
            "api.request.error", "application.started.time", "jvm.memory.max", "jvm.memory.used", "mq.heartbeat.last.sent.timestamp", "mq.heartbeat.last.failure.timestamp",
            "mq.heartbeat.failed", "mq.queue.size", "mq.queue.request.size", "mq.queue.response.size");

    public SystemMetricsController(MetricsEndpoint metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
    }

    @GetMapping(value = "/metrics", produces = "application/json")
    public ResponseEntity<Metrics> metrics() {
        Metrics metrics = new Metrics();
        for (String metricName : metricsNames) {
            MetricsEndpoint.MetricDescriptor metric = metricsEndpoint.metric(metricName, null);

            if (metric != null && !metric.getMeasurements().isEmpty()) {
                String name = metric.getName();
                String baseunit = metric.getBaseUnit();
                double value = metric.getMeasurements().get(0).getValue();
                String description = metric.getDescription();
                metrics.addSubject(new Metrics.MetricsItem(name, description, baseunit, String.valueOf(value)));
            }
        }

        return ResponseEntity.ok(metrics);
    }
}
