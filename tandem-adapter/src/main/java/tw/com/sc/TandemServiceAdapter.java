package tw.com.sc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PreDestroy;
import tw.com.sc.mq.MqConnectionManager;
@SpringBootApplication
@EnableScheduling
public class TandemServiceAdapter {
    public static void main(String[] args) {
        SpringApplication.run(TandemServiceAdapter.class, args);
    }

    @Autowired
    private MqConnectionManager mqConnectionManager;
    

    @PreDestroy
    public void onShutdown() {
        if (mqConnectionManager != null) {
            mqConnectionManager.stopListening();
        }
    }
}