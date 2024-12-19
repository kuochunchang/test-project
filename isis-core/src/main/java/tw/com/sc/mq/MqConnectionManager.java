package tw.com.sc.mq;

import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ibm.mq.jms.MQConnectionFactory;

import tw.com.sc.model.MqConfig;
import tw.com.sc.mq.metrics.MqServiceMetrics;

@Component
public class MqConnectionManager implements DisposableBean {

    private final MqConfig mqConfig;
    private final Logger logger = LoggerFactory.getLogger(MqConnectionManager.class);
    private final MqServiceMetrics mqServiceMetrics;

    private Connection connection;
    private Session session;
    private Session heartbeatSession;
    private MessageProducer requestProducer;
    private MessageProducer responseProducer;
    private Queue requestQueue;
    private Queue responseQueue;
    private volatile boolean isListening = false;
    private Thread listenerThread;
    private boolean isMqConnected = false;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private final MQConnectionFactory connectionFactory;

    private static final String TRANSACTION_CORRELATION_ID = "TX";

    private final Object connectionLock = new Object();
    private volatile boolean needReconnect = false;

    public MqConnectionManager(MqConfig mqConfig, MqServiceMetrics mqServiceMetrics,
            MQConnectionFactory connectionFactory) {
        this.mqConfig = mqConfig;
        this.mqServiceMetrics = mqServiceMetrics;
        this.connectionFactory = connectionFactory;

        try {
            initMqConnection();
        } catch (Exception e) {
            logger.error("初始化 MQ 連線時發生錯誤: {}", e.getMessage());
            logger.error("詳細錯誤:", e);
            throw new RuntimeException("MQ 連線初始化失�", e);
        }
    }

    private void initMqConnection() {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                logger.info("嘗試建立MQ連線 (第{}次)...", retryCount + 1);

                if (mqConfig.getSsl() != null) {
                    if (mqConfig.getSsl().getKeyStore() != null && 
                        mqConfig.getSsl().getKeyStorePassword() != null) {
                        System.setProperty("javax.net.ssl.keyStore", mqConfig.getSsl().getKeyStore());
                        System.setProperty("javax.net.ssl.keyStorePassword", mqConfig.getSsl().getKeyStorePassword());
                    }
                    
                    if (mqConfig.getSsl().getTrustStore() != null && 
                        mqConfig.getSsl().getTrustStorePassword() != null) {
                        System.setProperty("javax.net.ssl.trustStore", mqConfig.getSsl().getTrustStore());
                        System.setProperty("javax.net.ssl.trustStorePassword", mqConfig.getSsl().getTrustStorePassword());
                    }
                    
                    if (mqConfig.getSsl().getCipherSuite() != null) {
                        connectionFactory.setSSLCipherSuite(mqConfig.getSsl().getCipherSuite());
                    }
                }

                connection = connectionFactory.createConnection();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                heartbeatSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                requestQueue = session.createQueue(mqConfig.getQueue().getRequest());
                responseQueue = session.createQueue(mqConfig.getQueue().getResponse());
                requestProducer = session.createProducer(requestQueue);
                requestProducer.setTimeToLive(mqConfig.getConnectionTimeout());
                requestProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                responseProducer = session.createProducer(responseQueue);
                responseProducer.setTimeToLive(mqConfig.getConnectionTimeout());
                responseProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                connection.start();
                logger.info("MQ連線建立成功");
                isMqConnected = true;
                return;

            } catch (Exception e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    logger.error("MQ連線建立失敗，已重試{}次: {}", maxRetries, e.getMessage());
                    throw new MqTransactionManagerException("MQ連線建立失敗", e);
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MqTransactionManagerException("MQ連線重試被中斷", ie);
                }
            }
        }
    }

    @Scheduled(fixedRateString = "30000")
    private void recordQueueSize() {
        try {
            // 分別檢查請求和回應佇列
            recordSingleQueueSize(requestQueue, mqConfig.getQueue().getRequest());
            recordSingleQueueSize(responseQueue, mqConfig.getQueue().getResponse());
        } catch (Exception e) {
            logger.warn("檢查佇列大小時發生錯誤: {}", e.getMessage());
        }
    }

    private void recordSingleQueueSize(Queue queue, String queueName) {
        try {
            // MessageConsumer tempConsumer = session.createConsumer(queue);
            javax.jms.QueueBrowser browser = session.createBrowser(queue);
            int queueSize = Collections.list((Enumeration<?>) browser.getEnumeration()).size();
            if (queueName.equals(mqConfig.getQueue().getRequest())) {
                mqServiceMetrics.recordRequestQueueSize(queueSize);
            }
            if (queueName.equals(mqConfig.getQueue().getResponse())) {
                mqServiceMetrics.recordResponseQueueSize(queueSize);
            }
            logger.debug("佇列 {} 目前大小: {}", queueName, queueSize);
            browser.close();
            // tempConsumer.close();
        } catch (JMSException e) {
            logger.warn("無法取得佇列 {} 大小: {}", queueName, e.getMessage());
        }
    }

    @Scheduled(fixedRateString = "${mq.heartbeatInterval}")
    private void heartbeat() {
        // 檢查當前連線狀態
        if (heartbeatSession == null || connection == null) {
            isMqConnected = false;
            handleReconnection();
            return;
        }

        MessageConsumer consumer = null;
        try {
            // 先嘗試建立 consumer 來測試連線
            try {
                String correlationId = "HB" + System.currentTimeMillis();
                javax.jms.Message message = heartbeatSession.createTextMessage("HEARTBEAT");
                message.setJMSCorrelationID(correlationId);
                message.setJMSExpiration(mqConfig.getHeartbeatInterval());

                long sendTimeout = mqConfig.getHeartbeatInterval();
                requestProducer.setTimeToLive(sendTimeout);
                requestProducer.send(message,
                        javax.jms.DeliveryMode.NON_PERSISTENT,
                        javax.jms.Message.DEFAULT_PRIORITY,
                        sendTimeout);

                logger.debug("MQ發送心跳檢查訊息成功: correlationId={}", correlationId);

                consumer = heartbeatSession.createConsumer(
                        responseQueue,
                        "JMSCorrelationID='" + correlationId + "'");

                long timeout = mqConfig.getHeartbeatInterval();
                javax.jms.Message response = consumer.receive(timeout);

                if (response != null) {
                    isMqConnected = true;
                    mqServiceMetrics.recordHeartbeatReceivedTimestamp();
                    logger.debug("MQ心跳檢查成功，correlationId: {}", correlationId);
                } else {
                    throw new JMSException("心跳檢查超時");
                }

            } catch (JMSException e) {
                isMqConnected = false;
                mqServiceMetrics.incrementHeartbeatFailed();
                logger.warn("MQ心跳檢查失敗: {}", e.getMessage());
                handleReconnection();
            }

        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    logger.warn("關閉 consumer 時發生錯誤", e);
                }
            }
        }
    }

    public String sendTxMessageAsynchronous(String message) throws JMSException {

        mqServiceMetrics.incrementMqMessagesSent();

        javax.jms.TextMessage mqMessage = session.createTextMessage();
        mqMessage.setText(message);
        mqMessage.setJMSCorrelationID(TRANSACTION_CORRELATION_ID);
        mqMessage.setJMSExpiration(System.currentTimeMillis() + mqConfig.getTransactionExpiry());

        responseProducer.send(mqMessage);
        logger.debug("MQ 發送訊息成功: correlationId={}", mqMessage.getJMSCorrelationID());

        return mqMessage.getJMSMessageID();
    }

    public void putResponse(String message, String correlationId) {
        try {
            mqServiceMetrics.incrementMqMessagesSent();

            javax.jms.TextMessage mqMessage = session.createTextMessage();
            mqMessage.setText(message);
            mqMessage.setJMSCorrelationID(correlationId);
            // 設置訊息過期的時間
            mqMessage.setJMSExpiration(System.currentTimeMillis() + mqConfig.getTransactionExpiry());

            responseProducer.send(mqMessage);
            logger.info("MQ 發送訊息成功: correlationId={}", mqMessage.getJMSCorrelationID());

        } catch (JMSException e) {
            logger.error("發送Tandem回應訊息時發生錯誤: {}", e.getMessage());
            logger.error("Tandem回應訊息: {}", message);
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public String sendTxMessageAndWaitForResponse(String txRequest) throws JMSException, SyncTxTimeoutException {
        mqServiceMetrics.incrementMqMessagesSent();

        // 建立並發送請求訊息
        javax.jms.TextMessage requestMsg = session.createTextMessage();
        requestMsg.setText(txRequest);
        requestMsg.setJMSCorrelationID(TRANSACTION_CORRELATION_ID);
        requestMsg.setStringProperty("JMS_IBM_Character_Set", String.valueOf(mqConfig.getCcsid()));
        requestMsg.setJMSExpiration(System.currentTimeMillis() + mqConfig.getResponseWaitInterval());

        requestProducer.send(requestMsg);
        logger.info("MQ發送訊息成功: messageId={}", requestMsg.getJMSMessageID());

        // 建立消費者等待回應
        MessageConsumer consumer = null;
        try {
            consumer = session.createConsumer(
                    responseQueue,
                    "JMSCorrelationID='" + requestMsg.getJMSMessageID() + "'");

            // 接收回應
            Message responseMsg = consumer.receive(mqConfig.getResponseWaitInterval());

            if (responseMsg == null) {
                mqServiceMetrics.incrementMqTimeouts();
                String errorMsg = "MQ 接收訊息超時";
                logger.error("{} - 訊息ID: {}, 等待時間: {} ms", 
                    errorMsg, 
                    requestMsg.getJMSMessageID(), 
                    mqConfig.getResponseWaitInterval()
                );
                throw new SyncTxTimeoutException(
                    errorMsg,
                    requestMsg.getJMSMessageID(),
                    mqConfig.getResponseWaitInterval()
                );
            }

            String responseText;
            if (responseMsg instanceof TextMessage textMessage) {
                responseText = textMessage.getText();
            } else if (responseMsg instanceof BytesMessage bytesMessage) {
                byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);
                responseText = new String(bytes);
            } else {
                logger.error("不支援的訊息類型: {}", responseMsg.getClass().getName());
                throw new RuntimeException("不支援的訊息類型");
            }

            logger.info("MQ接收訊息成功: correlationId={}", responseMsg.getJMSCorrelationID());

            return responseText;

        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    logger.warn("關閉 consumer 時發生錯誤", e);
                }
            }
        }
    }

    // TODO 改成非同步的寫法會比較好
    public void startListeningTxMessage(MessageHandler messageHandler) {
        if (isListening) {
            logger.warn("監聽器已在運行中");
            return;
        }

        isListening = true;
        listenerThread = new Thread(() -> {
            while (isListening) {
                try {
                    MessageConsumer consumer = null;
                    try {
                        consumer = session.createConsumer(
                                requestQueue,
                                "JMSCorrelationID='" + TRANSACTION_CORRELATION_ID + "'");

                        // 使用 receiveNoWait 或設定較短的超時時間
                        javax.jms.Message message = consumer.receive(1000); // 設定1秒超時
                        if (message != null) {
                            messageHandler.onRequestMessage(message);
                        }

                    } finally {
                        if (consumer != null) {
                            try {
                                consumer.close();
                            } catch (JMSException e) {
                                logger.warn("關閉 consumer 時發生錯誤", e);
                            }
                        }
                    }

                    // 加入短暫休眠，避免過度消耗CPU
                    Thread.sleep(100);

                } catch (Exception e) {
                    logger.error("監聽佇列時發生錯誤: {}", e.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        listenerThread.start();
        logger.info("已開始監聽佇列");
    }

    public void stopListening() {
        if (!isListening) {
            return;
        }

        logger.info("正在停止 MQ 監聽...");
        isListening = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                // 最多等待 5 秒讓執行緒結束
                listenerThread.join(5000);
            } catch (InterruptedException e) {
                logger.warn("等待監聽執行緒結束時被中斷");
                Thread.currentThread().interrupt();
            }
            listenerThread = null;
        }
        logger.info("MQ 監聽已停止");
    }

    private void handleReconnection() {
        if (!isReconnecting.compareAndSet(false, true)) {
            logger.info("其他執行序正在重新連線中，跳過此次重連");
            return;
        }

        try {
            synchronized (connectionLock) {
                closeConnections();
                Thread.sleep(5000);

                try {
                    initMqConnection();
                    needReconnect = false;
                    logger.info("MQ連線已重新建立成功");
                } catch (Exception e) {
                    logger.error("MQ重新連線失敗: {}", e.getMessage());
                    needReconnect = true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            isReconnecting.set(false);
        }
    }

    private void closeConnections() {
        logger.warn("關閉MQ連線...");
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
            logger.warn("MQ連線已關閉");
        } catch (Exception e) {
            logger.warn("關閉MQ連線時發生錯誤: {}", e.getMessage());
        }
    }

    public interface MessageHandler {
        void onRequestMessage(javax.jms.Message message);
    }

    public boolean isMqConnected() {
        return isMqConnected;
    }

    /**
     * 同步交易超時異常
     */
    public class SyncTxTimeoutException extends Exception {
        public SyncTxTimeoutException(String message, String messageId, long waitTime) {
            super(String.format("%s (訊息ID: %s, 等待時間: %d ms)", message, messageId, waitTime));
        }
    }
    

    @Override
    public void destroy() throws Exception {
        logger.info("正在關閉 MQ 連線...");
        stopListening();
        closeConnections();
    }
}
