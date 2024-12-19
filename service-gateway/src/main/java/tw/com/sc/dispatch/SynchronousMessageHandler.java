package tw.com.sc.dispatch;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tw.com.sc.errorcode.ReturnCode;
import tw.com.sc.model.txmsg.TandemMessageHelper;
import tw.com.sc.model.txmsg.TandemMessageRequest;
import tw.com.sc.model.txmsg.TandemMessageResponse;
import tw.com.sc.mq.MqConnectionManager;

@Component("twoway")
public class SynchronousMessageHandler implements MessageHandler {

    private final MqConnectionManager mqConnectionManager;
    private static final Logger logger = LoggerFactory.getLogger(SynchronousMessageHandler.class);

    public SynchronousMessageHandler(MqConnectionManager mqTransactionManager) {
        this.mqConnectionManager = mqTransactionManager;
    }

    @Override
    public TandemMessageResponse processRequest(TandemMessageRequest request) {
        String jsonMessage = null;
        try {
            jsonMessage = TandemMessageHelper.RequestSerializer.toJson(request);
            logger.info("收到交易請求，轉發訊息到 MQ: {}", jsonMessage);

            String responseTxMessage = mqConnectionManager.sendTxMessageAndWaitForResponse(jsonMessage);

            logger.info("從 MQ 收到交易回應: {}", responseTxMessage);
            TandemMessageResponse response = TandemMessageHelper.ResponseSerializer.fromJson(responseTxMessage);
            if(response.getResultCode() == null) { 
                response.setResultCode(ReturnCode.SUCCESS.getCode());
                response.setResultMessage(ReturnCode.SUCCESS.getMessage());
            }
            return response;
        } catch (JMSException e) {
            logger.error("MQ 連線錯誤: {}", request, e);
            logger.error("發送交易失敗: {}", jsonMessage);
            TandemMessageResponse response = TandemMessageHelper.buildResponse(request);
            response.setResultCode(ReturnCode.MQ_SEND_ERROR.getCode());
            response.setResultMessage(ReturnCode.MQ_SEND_ERROR.getMessage());
            return response;
        } catch (MqConnectionManager.SyncTxTimeoutException e) {
            logger.error("交易逾時: {}", request, e);
            logger.error("發送交易失敗: {}", jsonMessage);
            TandemMessageResponse response = TandemMessageHelper.buildResponse(request);
            response.setResultCode(ReturnCode.MQ_SYNC_TX_TIMEOUT.getCode());
            response.setResultMessage(ReturnCode.MQ_SYNC_TX_TIMEOUT.getMessage());
            return response;
        }

    }

}
