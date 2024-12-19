package tw.com.sc.model.txmsg;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TandemMessageResponse {

    @JsonProperty("IS-RESULT-CODE")
    private String resultCode;

    @JsonProperty("IS-RESULT-MESSAGE")
    private String resultMessage;

    @JsonProperty("MSG-IN-DATA")
    private MsgInData msgInData;

    @Data
    public static class MsgInData {

        @JsonProperty("MSG-SESSION-ID")
        private String msgSessionId;

        @JsonProperty("MSG-STRNKEY-NO")
        private String msgStrnkeyNo;

        @JsonProperty("MSG-FILLER")
        private String msgFiller;

        @JsonProperty("MSG-RETURN-CODE")
        private String msgReturnCode;

        @JsonProperty("MSG-TIME-SEQ")
        private String msgTimeSeq;

        @JsonProperty("MSG-TRAN-DIRECTION")
        private String msgTranDirection;

        @JsonProperty("MSG-LENGTH")
        private String msgLength;

        @JsonProperty("MSG-IN-MSG-DATA")
        private String msgInMsgData;
    }
}
