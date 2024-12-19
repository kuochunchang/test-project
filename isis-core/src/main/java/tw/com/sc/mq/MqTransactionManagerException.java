package tw.com.sc.mq;

import com.ibm.mq.MQException;

public class MqTransactionManagerException extends RuntimeException {
    public MqTransactionManagerException(MQException e) {
        super(e);
    }

    public MqTransactionManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
