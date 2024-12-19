package tw.com.sc.monitor.vo.metrix;

import lombok.Data;

@Data
public class MetrixDetail {
    private Status status = new Status();
    private ApiMetrics api = new ApiMetrics();
    private MqMetrics mq = new MqMetrics();


    @Data
    public static class Status {
        private String systemStatus;
    }

    @Data
    public static class ApiMetrics {
        private String requestLatency;
        private String requestTotal;
        private String requestError;
    }

    @Data
    public static class MqMetrics {
        private String heartbeatFailedCount;
        private String heartbeatLastFailureTimestamp;
        private String heartbeatLastSentTimestamp;
        private String messagesSentCount;
        private String processingTime;
        private String queueRequestSize;
        private String queueResponseSize;
        private String responseTimeoutsCount;
    }
}
