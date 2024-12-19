package tw.com.sc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tw.com.sc.monitor.config.MonitoringTargetConfig;
import tw.com.sc.monitor.vo.metrix.MetrixDetail;

/**
 * 指標收集器類別
 * 負責從 Service Gateway 和 Tandem Adapter 收集各項監控指標
 * 包括 API 相關指標（請求數、錯誤數、延遲時間）和 MQ 相關指標（心跳、訊息處理、佇列狀態等）
 */
@Slf4j
public class MetricsCollector {

    private final MonitoringTargetConfig.Subject subject;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Service Gateway 的監控指標項目列表
     * 這些指標是從 Service Gateway 自己收集，然後透過 actuator 端點提供外部取得
     */
    private final String[] serviceGatewayMetricsItems = {
            "application.started.time",    // 應用程式啟動時間
            "api.requests.total",          // API 請求總數
            "api.requests.errors",         // API 錯誤請求數
            "api.request.latency",         // API 請求延遲時間
            "jvm.buffer.memory.used",      // JVM 緩衝區記憶體使用量
            "mq.heartbeat.failed",         // MQ 心跳失敗次數
            "mq.heartbeat.last.failure.timestamp",  // 最後一次心跳失敗時間戳
            "mq.heartbeat.last.sent.timestamp",     // 最後一次發送心跳時間戳
            "mq.messages.sent",            // MQ 訊息發送數量
            "mq.processing.time",          // MQ 訊息處理時間
            "mq.queue.request.size",       // MQ 請求佇列大小
            "mq.queue.response.size",      // MQ 回應佇列大小
            "mq.timeouts",                 // MQ 超時次數
    };

    /**
     * Tandem Adapter 的監控指標項目列表
     */
    private final String[] tandemAdapterMetricsItems = {
            "application.started.time",    // 應用程式啟動時間
            "jvm.buffer.memory.used",      // JVM 緩衝區記憶體使用量
            "mq.heartbeat.failed",         // MQ 心跳失敗次數
            "mq.heartbeat.last.failure.timestamp",  // 最後一次心跳失敗時間戳
            "mq.heartbeat.last.sent.timestamp",     // 最後一次發送心跳時間戳
            "mq.messages.sent",            // MQ 訊息發送數量
            "mq.processing.time",          // MQ 訊息處理時間
            "mq.queue.request.size",       // MQ 請求佇列大小
            "mq.queue.response.size",      // MQ 回應佇列大小
            "mq.timeouts",                 // MQ 超時次數
    };

    public MetricsCollector(MonitoringTargetConfig.Subject subject) {
        this.subject = subject;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 獲取 Service Gateway 的原始指標數據
     * @return 原始指標數據字串
     */
    public String fetchServiceGatewayMetrics() {
        return fetchMetrics(subject.getServiceGateway().getMetrics().getEndpoint(), serviceGatewayMetricsItems);
    }

    /**
     * 獲取 Tandem Adapter 的原始指標數據
     * @return 原始指標數據字串
     */
    public String fetchTandemAdapterMetrics() {
        return fetchMetrics(subject.getTandemAdapter().getMetrics().getEndpoint(), tandemAdapterMetricsItems);
    }

    /**
     * 獲取 Service Gateway 的指標數據，並轉換為 MetrixDetail 物件
     * @return 解析後的 MetrixDetail 物件
     */
    public MetrixDetail fetchServiceGatewayMetricsAsDetail() {
        String metricsData = fetchServiceGatewayMetrics();
        MetrixDetail detail = new MetrixDetail();
        parseMetricsToDetail(metricsData, detail);
        parseHealthStatus(fetchHealth(subject.getServiceGateway().getMetrics().getEndpoint()), detail);
        return detail;
    }

    /**
     * 獲取 Tandem Adapter 的指標數據，並轉換為 MetrixDetail 物件
     * @return 解析後的 MetrixDetail 物件
     */
    public MetrixDetail fetchTandemAdapterMetricsAsDetail() {
        String metricsData = fetchTandemAdapterMetrics();
        MetrixDetail detail = new MetrixDetail();
        parseMetricsToDetail(metricsData, detail);
        parseHealthStatus(fetchHealth(subject.getTandemAdapter().getMetrics().getEndpoint()), detail);
        return detail;
    }

    /**
     * 從指定端點獲取指標數據
     * @param endpoint 指標數據端點 URL
     * @param metricsItems 要獲取的指標項目列表
     * @return 所有指標數據的字串
     */
    private String fetchMetrics(String endpoint, String[] metricsItems) {
        StringBuilder result = new StringBuilder();
        
        for (String item : metricsItems) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/metrics/" + item))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                result.append(response.body()).append("\n");
            } catch (Exception e) {
                log.error("從 {} 獲取指標 {} 時發生錯誤: {}", endpoint, item, e.getMessage());
            }
        }
        
        return result.toString();
    }

    /**
     * 獲取 Service Gateway 的健康狀態
     * @param endpoint 健康狀態端點 URL
     * @return 健康狀態字串
     */
    private String fetchHealth(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/health"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body(); // 健康狀態字串
        } catch (Exception e) {
            log.error("從 {} 獲取健康狀態時發生錯誤: {}", endpoint, e.getMessage());
            return null;
        }
    }

    private void parseHealthStatus(String healthData, MetrixDetail detail) {
        try {
            JsonNode node = objectMapper.readTree(healthData);
            detail.getStatus().setSystemStatus(node.get("status").asText());
        } catch (JsonProcessingException e) {
            log.error("解析健康狀態時發生錯誤", e);
        }
    }
    

    /**
     * 解析指標數據並填入 MetrixDetail 物件
     * @param metricsData 原始指標數據字串（每行一個 JSON）
     * @param detail 要填入數據的 MetrixDetail 物件
     */
    private void parseMetricsToDetail(String metricsData, MetrixDetail detail) {
        try {
            String[] metrics = metricsData.split("\n");
            for (String metric : metrics) {
                JsonNode node = objectMapper.readTree(metric);
                String name = node.get("name").asText();
                JsonNode measurements = node.get("measurements");
                
                switch (name) {
                    case "api.request.latency":  // API 請求延遲時間
                        detail.getApi().setRequestLatency(getMetricValue(measurements, "TOTAL_TIME"));
                        break;
                    case "api.requests.total":   // API 請求總數
                        detail.getApi().setRequestTotal(getMetricValue(measurements, "COUNT"));
                        break;
                    case "api.requests.errors":  // API 錯誤請求數
                        detail.getApi().setRequestError(getMetricValue(measurements, "COUNT"));
                        break;
                    case "mq.heartbeat.failed": // MQ 心跳失敗次數
                        detail.getMq().setHeartbeatFailedCount(getMetricValue(measurements, "COUNT"));
                        break;
                    case "mq.heartbeat.last.failure.timestamp": // 最後一次心跳失敗時間戳
                        detail.getMq().setHeartbeatLastFailureTimestamp(getMetricValue(measurements, "VALUE"));
                        break;
                    case "mq.heartbeat.last.sent.timestamp":    // 最後一次發送心跳時間戳
                        detail.getMq().setHeartbeatLastSentTimestamp(getMetricValue(measurements, "VALUE"));
                        break;
                    case "mq.messages.sent":     // MQ 訊息發送數量
                        detail.getMq().setMessagesSentCount(getMetricValue(measurements, "COUNT"));
                        break;
                    case "mq.processing.time":   // MQ 訊息處理時間
                        detail.getMq().setProcessingTime(getMetricValue(measurements, "TOTAL_TIME"));
                        break;
                    case "mq.queue.request.size":    // MQ 請求佇列大小
                        detail.getMq().setQueueRequestSize(getMetricValue(measurements, "VALUE"));
                        break;
                    case "mq.queue.response.size":   // MQ 回應佇列大小
                        detail.getMq().setQueueResponseSize(getMetricValue(measurements, "VALUE"));
                        break;
                    case "mq.timeouts":         // MQ 超時次數
                        detail.getMq().setResponseTimeoutsCount(getMetricValue(measurements, "COUNT"));
                        break;
                }
            }
        } catch (Exception e) {
            log.error("解析指標數據時發生錯誤", e);
        }
    }

    /**
     * 從 measurements 陣列中取得指定統計類型的值
     * @param measurements JSON 陣列節點，包含多個統計值
     * @param statistic 要查找的統計類型（如 "COUNT", "VALUE", "TOTAL_TIME" 等）
     * @return 找到的統計值，如果未找到則返回 null
     */
    private String getMetricValue(JsonNode measurements, String statistic) {
        for (JsonNode measurement : measurements) {
            if (measurement.get("statistic").asText().equals(statistic)) {
                return measurement.get("value").asText();
            }
        }
        return null;
    }
}
