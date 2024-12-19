package tw.com.sc.client;

public class TandemApiException extends RuntimeException {
    private final String errorCode;
    
    public TandemApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TandemApiException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
} 