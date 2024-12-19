package tw.com.sc.dispatch;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MessageHandlerConfig {

    @Bean
    @Primary
    public MessageHandler messageHandler(
            @Value("${transaction.message-handler}") String handlerType,
            @Qualifier("oneway") MessageHandler onewayHandler,
            @Qualifier("twoway") MessageHandler twowayHandler) {

        return "oneway".equals(handlerType) ? onewayHandler : twowayHandler;
    }
}