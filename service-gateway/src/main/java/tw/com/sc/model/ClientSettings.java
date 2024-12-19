package tw.com.sc.model;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties
@Data
public class ClientSettings {
    private List<Client> clients;
    
    @Data
    public static class Client {
        private String id;
        private String secret;
    }
}