package tw.com.sc.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Metrics {
    private List<MetricsItem> subjects = new ArrayList<>();
    public void addSubject(MetricsItem item) {
        subjects.add(item);
    }

    @Data
    @AllArgsConstructor
    public static class MetricsItem {
        private String name;
        private String description;
        private String baseunit;
        private String value;
    }
}
