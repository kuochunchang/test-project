package tw.com.sc;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import tw.com.sc.monitor.config.MonitoringTargetConfig;
import tw.com.sc.monitor.email.MailContent;
import tw.com.sc.monitor.email.SendMailService;
import tw.com.sc.monitor.vo.metrix.MetrixData;


/**
 * 系統監控主程式
 * 
 * 1. 使用外部 cron 排程執行，收集各個監控目標(多個不同系統)的指標
 * 2. 透過 email 發佈
 * 程式分成三個部分:
 * 1. 監控目標的設定值
 * 2. 指標的收集
 * 3. 指標的發佈
 */

@SpringBootApplication
@EnableConfigurationProperties(MonitoringTargetConfig.class)
public class SystemMonitor {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitor.class);

    public static void main(String[] args) {
        SpringApplication.run(SystemMonitor.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(MonitoringTargetConfig subjectConfig, SendMailService mailSender) {
        List<MetrixData> metrixDataList = new ArrayList<>();
        
        return args -> {
            // 為每個監控目標(多個不同系統)建立指標收集器
            // 內容分成兩個部分:
            // 1. Service Gateway 指標
            // 2. Tandem Adapter 指標
            for (MonitoringTargetConfig.Subject subject : subjectConfig.getSubjects()) {
                MetricsCollector collector = new MetricsCollector(subject);
                MetrixData metrixData = new MetrixData();
                metrixData.setFunctionName(subject.getName());
                try {
                    // 收集 Service Gateway 指標
                    metrixData.setGateway(collector.fetchServiceGatewayMetricsAsDetail());
                    
                    // 收集 Tandem Adapter 指標
                    metrixData.setTandemAdapter(collector.fetchTandemAdapterMetricsAsDetail());

                    metrixDataList.add(metrixData);
                } catch (Exception e) {
                    log.error("收集指標時發生錯誤，監控目標: " + subject.getName(), e);
                }
            }

            log.info(metrixDataList.toString());

            if (!metrixDataList.isEmpty()) {
                MailContent mailContent = MailContent.createFromMetrixData(metrixDataList);
                mailSender.sendMail(mailContent);
            }
        };
    }

    

}