package tw.com.sc.model;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "filters")
@Component
@Data
public class FilterConfig {
    private List<String> strnkeys;
}
