package tw.com.sc.model;

import javax.jms.JMSException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ConfigurationProperties(prefix = "mq")
@Component
@Data
@Slf4j
public class MqConfig {
    private String host;
    private int port;
    private String qmgr;
    private String channelName;
    private QueueConfig queue;
    private int ccsid;
    private int heartbeatInterval;
    private int transactionExpiry;
    private int responseWaitInterval;

    private int maxConnections = 10;
    private int maxSessionsPerConnection = 5;
    private int connectionTimeout = 30000;

    private SslConfig ssl;

    @Bean
    public MQConnectionFactory mqConnectionFactory() {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(host);
        try {
            factory.setPort(port);
            factory.setQueueManager(qmgr);
            factory.setChannel(channelName);
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setSSLCipherSuite(ssl.getCipherSuite());
            factory.setSSLPeerName(ssl.getSslPeerName());
            log.info("使用的加密套件: {}", ssl.getCipherSuite());
            factory.setSSLFipsRequired(false);
            factory.setStringProperty("SSL_KEYSTORE", ssl.getKeyStore());
            factory.setStringProperty("SSL_KEYSTORE_PASSWORD", ssl.getKeyStorePassword());
            factory.setStringProperty("SSL_TRUSTSTORE", ssl.getTrustStore());
            factory.setStringProperty("SSL_TRUSTSTORE_PASSWORD", ssl.getTrustStorePassword());
            factory.setStringProperty("SSL_KEYSTORE_TYPE", ssl.getKeyStoreType());
            factory.setStringProperty("SSL_TRUSTSTORE_TYPE", ssl.getTrustStoreType());
        } catch (JMSException e) {
            log.error("MQ連線失敗，使用的加密套件: {}", ssl.getCipherSuite(), e);
            throw new RuntimeException("MQ連線工廠建立失敗", e);
        }
        return factory;
    }

    @Data
    public static class QueueConfig {
        private String request;
        private String response;
    }

    @Data
    public static class SslConfig {
        private boolean enabled;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
        private String cipherSuite;
        private String sslPeerName;
        private String keyStoreType;
        private String trustStoreType;
    }
}
