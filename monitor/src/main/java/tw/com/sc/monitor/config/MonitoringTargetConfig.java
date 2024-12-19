package tw.com.sc.monitor.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties
public class MonitoringTargetConfig {
    private List<Subject> subjects = new ArrayList<>();
    
    @Data
    public static class Subject {
        private String name;
        private ServiceGateway serviceGateway;
        private TandemAdapter tandemAdapter;
    }
    
    @Data
    public static class ServiceGateway {
        private Metrics metrics;
    }
    
    @Data
    public static class TandemAdapter {
        private Metrics metrics;
    }
    
    @Data
    public static class Metrics {
        private String endpoint;
    }
}
