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

@Component("oneway")
public class ImmediateResponseHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ImmediateResponseHandler.class);
    private final MqConnectionManager mqConnectionManager;

    public ImmediateResponseHandler(MqConnectionManager mqConnectionManager) {
        this.mqConnectionManager = mqConnectionManager; 
    }

    @Override
    public TandemMessageResponse processRequest(TandemMessageRequest message) {
        try {
            String jsonMessage = TandemMessageHelper.RequestSerializer.toJson(message);
            logger.info("收到交易請求，轉發訊息到 MQ: {}", jsonMessage);
            mqConnectionManager.sendTxMessageAsynchronous(jsonMessage);
            // 回傳成功訊息
            TandemMessageResponse response = TandemMessageHelper.buildResponse(message);
            response.setResultCode(ReturnCode.SUCCESS.getCode());
            response.setResultMessage(ReturnCode.SUCCESS.getMessage());
            logger.info("訊息已轉發 sessionId: {}", message.getMsgInData().getMsgSessionId());
            return response;
            
        } catch (JMSException e) {
            logger.error("MQ發送訊息失敗(errorCode={}): {} {}", e.getErrorCode(), e.getMessage(), message);
            TandemMessageResponse response = TandemMessageHelper.buildResponse(message);
            response.setResultCode(ReturnCode.MQ_SEND_ERROR.getCode());
            response.setResultMessage(ReturnCode.MQ_SEND_ERROR.getMessage());
            return response;
        }
    }
}
