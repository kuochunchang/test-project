package tw.com.sc.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "tandem")
public class TandemConfig {
    private ApiConfig api;

    @Data
    public static class ApiConfig {
        private String url;
        private String username;
        private String password;
    }
}


