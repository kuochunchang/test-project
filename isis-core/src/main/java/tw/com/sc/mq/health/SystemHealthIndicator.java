package tw.com.sc.mq.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import tw.com.sc.mq.MqConnectionManager;

@Component
public class SystemHealthIndicator implements HealthIndicator {

    private MqConnectionManager mqTransactionManager;

    public SystemHealthIndicator(MqConnectionManager mqTransactionManager) {
        this.mqTransactionManager = mqTransactionManager;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {

            boolean mqStatus = checkMqHealth();

            if (mqStatus) {
                return builder.up()
                        .withDetail("mq", "UP")
                        .build();
            } else {
                builder = builder.down();
                if (!mqStatus) {
                    builder.withDetail("mq", "DOWN");
                }
                return builder.build();
            }
        } catch (Exception e) {
            return builder.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkMqHealth() {
        try {
            return mqTransactionManager.isMqConnected();
        } catch (Exception e) {
            return false;
        }
    }
}