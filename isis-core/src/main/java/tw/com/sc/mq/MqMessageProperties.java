package tw.com.sc.mq;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQMessage;

import lombok.Data;

@Data
public class MqMessageProperties {
    private byte[] messageId;
    private byte[] correlationId;
    private String payload;

    private static Logger logger = LoggerFactory.getLogger(MqMessageProperties.class);

    public static MqMessageProperties of(MQMessage mqMessage) {
        MqMessageProperties messageProperties = new MqMessageProperties();
        try {
            messageProperties.setMessageId(mqMessage.messageId);
            messageProperties.setCorrelationId(mqMessage.correlationId);

            // 確保資料指標指向訊息主體的開始位置
            mqMessage.seek(0);

            // 獲取剩餘的可讀資料長度
            int dataLength = mqMessage.getDataLength();

            byte[] data = new byte[dataLength];
            mqMessage.readFully(data);

            String messageContent = new String(data, "UTF-8");
            messageProperties.setPayload(messageContent);

        } catch (IOException e) {
            logger.error("將 MQMessage 資料轉換為字串失敗", e);
            return new MqMessageProperties();
        }

        return messageProperties;
    }

    /**
     * 將 byte array 轉換為 HEX 字串
     * 
     * @param array
     * @return
     */
    private static String byteArrayToString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[messageId=" + byteArrayToString(messageId) + ", correlationId=" + byteArrayToString(correlationId)
                + ", payload=" + payload + "]";

    }
}
