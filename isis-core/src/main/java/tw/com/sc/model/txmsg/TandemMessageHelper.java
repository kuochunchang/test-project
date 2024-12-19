package tw.com.sc.model.txmsg;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;



public class TandemMessageHelper {

    private TandemMessageHelper() {}

    public static TandemMessageResponse buildResponse(TandemMessageRequest request) {
        TandemMessageResponse response = new TandemMessageResponse();
        response.setMsgInData(new TandemMessageResponse.MsgInData());

        BeanUtils.copyProperties(request.getMsgInData(), response.getMsgInData());
        return response;
    }

    public class RequestSerializer {


        private RequestSerializer() {}
        private static final ObjectMapper objectMapper = new ObjectMapper();

        public static String toJson(TandemMessageRequest message) {
            try {
                return objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize to JSON: " + e.getMessage(), e);
            }
        }

        public static TandemMessageRequest fromJson(String json) {
            try {
                return objectMapper.readValue(json, TandemMessageRequest.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize from JSON: " + e.getMessage(), e);
            }
        }

        public static TandemMessage getMessageBody(TandemMessageRequest request) {
            if (request == null || request.getMsgInData() == null) {
                throw new IllegalArgumentException("請求或請求內容不能為空");
            }

            TandemMessage tandemMessage = new TandemMessage();
            tandemMessage.setMsgInData(new TandemMessage.MsgInData());
            BeanUtils.copyProperties(request.getMsgInData(), tandemMessage.getMsgInData());
            return tandemMessage;
        }
    }

    public class ResponseSerializer {

        private static final ObjectMapper objectMapper = new ObjectMapper();
    
        private ResponseSerializer() {
        }
    
        public static TandemMessageResponse fromJson(String json) {
            try {
                return objectMapper.readValue(json, TandemMessageResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
            }
        }
    
        public static String toJson(TandemMessageResponse message) {
            try {
                return objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize to JSON: " + e.getMessage(), e);
            }
        }
    }
    
}
