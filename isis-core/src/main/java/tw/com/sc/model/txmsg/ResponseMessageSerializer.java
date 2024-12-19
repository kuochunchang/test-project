package tw.com.sc.model.txmsg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMessageSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseMessageSerializer() {
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
