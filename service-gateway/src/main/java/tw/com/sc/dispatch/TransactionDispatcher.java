package tw.com.sc.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tw.com.sc.model.txmsg.TandemMessageRequest;
import tw.com.sc.model.txmsg.TandemMessageResponse;

/**
 * 電文處理
 * 1. 電文格式轉換
 * 2. 電文轉發
 * 3. 電文回應
 */

@Service
public class TransactionDispatcher {
    private final MessageHandler messageHandler;
    private static final Logger logger = LoggerFactory.getLogger(TransactionDispatcher.class);

    public TransactionDispatcher(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;

        logger.info("使用的 messageHandler:{} {}", messageHandler.getClass().getName());
    }

    public TandemMessageResponse forwardRequest(TandemMessageRequest message) {
        return messageHandler.processRequest(message);
    }
}
