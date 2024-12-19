package tw.com.sc.monitor.email;

import java.util.List;

import tw.com.sc.monitor.vo.metrix.MetrixData;

public class MailContent {
    private String subject;
    private String content;
    
    public static MailContent createFromMetrixData(List<MetrixData> metrixDataList) {
        MailContent mailContent = new MailContent();
        mailContent.subject = "系統監控指標報告";
        
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<html><body>");
        contentBuilder.append("<h2>系統監控指標報告</h2>");
        
        for (MetrixData data : metrixDataList) {
            contentBuilder.append("<h3>功能: ").append(data.getFunctionName()).append("</h3>");
            
            // Service Gateway 指標
            if (data.getGateway() != null) {
                contentBuilder.append("<h4>Service Gateway 指標:</h4>");
                contentBuilder.append("<pre>").append(data.getGateway().toString()).append("</pre>");
            }
            
            // Tandem Adapter 指標
            if (data.getTandemAdapter() != null) {
                contentBuilder.append("<h4>Tandem Adapter 指標:</h4>");
                contentBuilder.append("<pre>").append(data.getTandemAdapter().toString()).append("</pre>");
            }
            
            contentBuilder.append("<hr>");
        }
        
        contentBuilder.append("</body></html>");
        mailContent.content = contentBuilder.toString();
        
        return mailContent;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public String getContent() {
        return content;
    }
}
