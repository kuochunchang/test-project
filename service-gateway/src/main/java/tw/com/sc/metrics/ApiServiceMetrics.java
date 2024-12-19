package tw.com.sc.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class ApiServiceMetrics {
    private final Counter totalRequestsCounter;
    private final Counter errorRequestsCounter;
    private final Timer requestLatencyTimer;


    public ApiServiceMetrics(MeterRegistry registry) {
        // API 請求相關指標
        this.totalRequestsCounter = Counter.builder("api.requests.total")
            .description("total api requests count")
            .register(registry);
            
        this.errorRequestsCounter = Counter.builder("api.requests.errors")
            .description("api errors count")
            .register(registry);
            
        this.requestLatencyTimer = Timer.builder("api.request.latency")
            .description("api request latency")
            .register(registry);

    }

    // API 指標方法
    public void incrementTotalRequests() {
        totalRequestsCounter.increment();
    }

    public void incrementErrorRequests() {
        errorRequestsCounter.increment();
    }

    public Timer.Sample startRequestTimer() {
        return Timer.start();
    }

    public void stopRequestTimer(Timer.Sample sample) {
        sample.stop(requestLatencyTimer);
    }

} 