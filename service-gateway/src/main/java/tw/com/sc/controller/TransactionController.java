package tw.com.sc.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Timer.Sample;
import tw.com.sc.dispatch.TransactionDispatcher;
import tw.com.sc.errorcode.ReturnCode;
import tw.com.sc.metrics.ApiServiceMetrics;
import tw.com.sc.model.txmsg.TandemMessage;
import tw.com.sc.model.txmsg.TandemMessageHelper;
import tw.com.sc.model.txmsg.TandemMessageRequest;
import tw.com.sc.model.txmsg.TandemMessageResponse;

// TODO 電文的格式需要等到正式的規格獲得之後再做調整
@RestController
public class TransactionController {

    private final TransactionDispatcher transactionDispatcher;
    private final ApiServiceMetrics metrics;
    private final ClientValidator clientValidator;

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionDispatcher transactionManager, ApiServiceMetrics metrics,
            ClientValidator clientValidator) {
        this.transactionDispatcher = transactionManager;
        this.metrics = metrics;
        this.clientValidator = clientValidator;
    }

    @PostMapping(value = "/api/v1/submitTransaction", produces = "application/json;charset=UTF-8", consumes = "application/json;charset=UTF-8")
    public ResponseEntity<TandemMessageResponse> submitTransaction(@RequestBody TandemMessageRequest apiRequest) {
        metrics.incrementTotalRequests();
        Sample timer = metrics.startRequestTimer();
        boolean isSuccess = false;

        try {

            // 驗證 clientId 和 clientSecret
            if (!clientValidator.validate(apiRequest.getClientId(), apiRequest.getClientSecret())) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(buildErrorResponse(apiRequest, ReturnCode.UNAUTHORIZED.getCode(),
                                ReturnCode.UNAUTHORIZED.getMessage()));
            }

            if (apiRequest.getMsgInData() == null) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(buildErrorResponse(apiRequest, ReturnCode.BAD_REQUEST.getCode(),
                                ReturnCode.BAD_REQUEST.getMessage()));
            }

            // 如果沒有 sessionId 則產生一個， 以便於後續追蹤
            if (apiRequest.getMsgInData().getMsgSessionId() == null) {
                apiRequest.getMsgInData().setMsgSessionId(UUID.randomUUID().toString());
            }

            // 將交易轉發到 Tandem Adapter
            logger.info("API 收到請求 sessionId: {}", apiRequest.getMsgInData().getMsgSessionId());
            TandemMessageResponse response = transactionDispatcher.forwardRequest(apiRequest);
            isSuccess = true;


            // 如果 ISIS 處理失敗，則回傳 502
            if(!ReturnCode.SUCCESS.getCode().equals(response.getResultCode())) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
            }

            // 如果 ISIS 處理成功且 Tandem 也處理成功
            // Tandem 回應的結果
            String tandemResultCode = response.getMsgInData().getMsgReturnCode();
            // one way 的時候，Tandem 回應的結果是空字串
            if (TandemMessage.DEFAULT_SUCCESS_CODE.equals(tandemResultCode) || "".equals(tandemResultCode)) {
                response.setResultCode(ReturnCode.SUCCESS.getCode());
                response.setResultMessage(ReturnCode.SUCCESS.getMessage());
                logger.info("API 處理完成 sessionId: {}", apiRequest.getMsgInData().getMsgSessionId());
            } else {
                response.setResultCode(ReturnCode.WARNING.getCode());
                response.setResultMessage(ReturnCode.WARNING.getMessage());
                logger.warn("API 處理完成 sessionId: {}, 但交易失敗：{} {}", apiRequest.getMsgInData().getMsgSessionId(),
                        response.getResultCode(), response.getResultMessage());
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            logger.error("API 處理時發生錯誤: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(apiRequest, ReturnCode.SYSTEM_ERROR.getCode(),
                            ReturnCode.SYSTEM_ERROR.getMessage()));
        }

        finally {
            if (!isSuccess) {
                metrics.incrementErrorRequests();
            }
            metrics.stopRequestTimer(timer);
        }
    }

    private TandemMessageResponse buildErrorResponse(TandemMessageRequest request, String errorCode,
            String errorMessage) {
        TandemMessageResponse response = TandemMessageHelper.buildResponse(request);
        response.setResultCode(errorCode);
        response.setResultMessage(errorMessage);
        return response;
    }

}
