package tw.com.sc.dispatch;

import tw.com.sc.model.txmsg.TandemMessageRequest;
import tw.com.sc.model.txmsg.TandemMessageResponse;

public interface MessageHandler {
    TandemMessageResponse processRequest(TandemMessageRequest request);
}
