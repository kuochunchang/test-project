package tw.com.sc.monitor.vo.metrix;

import lombok.Data;

@Data
public class MetrixData {
    private String functionName;
    private MetrixDetail gateway = new MetrixDetail();
    private MetrixDetail tandemAdapter = new MetrixDetail();
}
