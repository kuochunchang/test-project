package tw.com.sc.errorcode;

public enum ReturnCode {

    SUCCESS("0000", "資料轉發（處理）成功"),
    WARNING("0099", "資料轉發（處理）成功，但 Tandem 回應錯誤"),
    
    // MQ 相關錯誤 (2001-2999) 
    MQ_CONNECTION_ERROR("2001", "MQ連線建立失敗"),
    MQ_SEND_ERROR("2002", "MQ發送訊息失敗"),
    MQ_RECEIVE_ERROR("2003", "MQ接收訊息失敗"),
    MQ_CONNECTION_TIMEOUT("2004", "MQ連線逾時"),
    MQ_SYNC_TX_TIMEOUT("2005", "MQ同步交易逾時"),
    
    // Tandem 相關錯誤 (3001-3999)
    TANDEM_CONNECTION_ERROR("3001", "Tandem連線失敗"),
    TANDEM_TIMEOUT_ERROR("3002", "Tandem處理逾時"),
    TANDEM_RESPONSE_ERROR("3003", "Tandem回應格式錯誤"),
    TANDEM_HTTP_ERROR("3004", "Tandem HTTP 錯誤"),
    TANDEM_REQUEST_ERROR("3005", "Tandem 請求發生錯誤"),

    // 業務邏輯錯誤 (4001-4999)
    INVALID_MESSAGE_FORMAT("4001", "無效的訊息格式"),
    MISSING_REQUIRED_FIELD("4002", "缺少必要欄位"),
    INVALID_SESSION_ID("4003", "無效的交易序號"),
    
    // API 錯誤 (5001-5999)
    UNAUTHORIZED("5001", "未授權"),
    BAD_REQUEST("5002", "錯誤的請求"),

    // 系統層級錯誤 (9000-9999)
    SYSTEM_ERROR("9000", "系統發生未預期錯誤");


    private final String code;
    private final String message;

    ReturnCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
