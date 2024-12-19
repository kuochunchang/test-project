package tw.com.sc;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tw.com.sc.client.TandemApiClient;
import tw.com.sc.client.TandemApiException;
import tw.com.sc.model.FilterConfig;
import tw.com.sc.model.TandemConfig;
import tw.com.sc.model.txmsg.TandemMessage;
import tw.com.sc.model.txmsg.TandemMessageHelper;
import tw.com.sc.model.txmsg.TandemMessageRequest;
import tw.com.sc.model.txmsg.TandemMessageResponse;
import tw.com.sc.model.txmsg.TandemMessageSerializer;
import tw.com.sc.mq.MqConnectionManager;

/**
 * 進行 Tandem 訊息處理
 * 錯誤訊息處理
 */
@Service
public class TandemMessageProcessor implements MqConnectionManager.MessageHandler {
    private final MqConnectionManager mqTransactionManager;

    private static final Logger logger = LoggerFactory.getLogger(TandemMessageProcessor.class);

    private final TandemApiClient tandemApiClient;
    private final FilterConfig filterConfig;

    public TandemMessageProcessor(
            MqConnectionManager mqTransactionManager,
            TandemConfig tabdemConfig,
            TandemApiClient tandemApiClient,
            FilterConfig filterConfig) {
        this.mqTransactionManager = mqTransactionManager;
        this.tandemApiClient = tandemApiClient;
        this.mqTransactionManager.startListeningTxMessage(this::onRequestMessage);
        this.filterConfig = filterConfig;
    }

    @Override
    public void onRequestMessage(javax.jms.Message message) {
        if (message == null) {
            logger.error("收到空的 MQ 訊息");
            return;
        }

        try {
            String strResponse = ((javax.jms.TextMessage) message).getText();
            if (strResponse == null || strResponse.trim().isEmpty()) {
                logger.error("MQ 訊息內容為空");
                return;
            }

            logger.info("收到MQ訊息: {}", strResponse);
            String correlationId = message.getJMSMessageID();

            TandemMessageRequest tandemRequestInIsis = TandemMessageHelper.RequestSerializer.fromJson(strResponse);
            if (tandemRequestInIsis == null || tandemRequestInIsis.getMsgInData() == null) {
                logger.error("無法解析 MQ 訊息內容: {}", strResponse);
                return;
            }

            TandemMessage tandemMessage = TandemMessageHelper.RequestSerializer.getMessageBody(tandemRequestInIsis);
            try {
                // Invoke Tandem API
                TandemMessage tandemResponse = tandemApiClient.callTandemApi(tandemMessage);


                // 依據 strnkey 過濾掉不需要回應的訊息
                String strnkey = tandemMessage.getMsgInData().getMsgStrnkeyNo();
                if (isStrnkeyFiltered(strnkey)) {
                    logger.info("Trnkey={} 的 Tandem 訊息不放進 MQ", strnkey);
                    return;
                }

                String tandemResponseJson = TandemMessageSerializer.toJson(tandemResponse);
                mqTransactionManager.putResponse(tandemResponseJson, correlationId);
                logger.info("Tandem 處理成功， 將回應放回 MQ: {}", tandemResponseJson);

            } catch (TandemApiException e) {
                logger.error("Tandem API 處理失敗: {}, 錯誤代碼: {}", e.getMessage(), e.getErrorCode());
                TandemMessageResponse tandemResponse = TandemMessageHelper.buildResponse(tandemRequestInIsis);
                tandemResponse.setResultCode(e.getErrorCode());
                tandemResponse.setResultMessage(e.getMessage());
                String tandemResponseJson = TandemMessageHelper.ResponseSerializer.toJson(tandemResponse);
                // 將錯誤訊息回傳
                mqTransactionManager.putResponse(tandemResponseJson, correlationId);
                
            }

        } catch (JMSException e) {
            logger.error("處理 MQ 訊息時發生錯誤: {}", e.getMessage());
            //logger.error(ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            logger.error("處理訊息時發生未預期的錯誤", e);
        }
    }

    private boolean isStrnkeyFiltered(String strnkey) {
        if (filterConfig.getStrnkeys() == null || filterConfig.getStrnkeys().isEmpty()) {
            return false;
        }
        return filterConfig.getStrnkeys().contains(strnkey);
    }
}
