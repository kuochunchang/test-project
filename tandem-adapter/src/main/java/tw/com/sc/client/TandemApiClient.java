package tw.com.sc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tw.com.sc.errorcode.ReturnCode;
import tw.com.sc.model.TandemConfig;
import tw.com.sc.model.txmsg.TandemMessage;

/**
 * Tandem API Client
 * 
 * 負責與Tandem API進行交互，處理請求和回應。
 */
@Component
public class TandemApiClient {
    private static final Logger logger = LoggerFactory.getLogger(TandemApiClient.class);

    private final RestTemplate restTemplate;
    private final String url;
    private final HttpHeaders headers;

    public TandemApiClient(TandemConfig tabdemConfig) {

        this.restTemplate = new RestTemplate();
        this.url = tabdemConfig.getApi().getUrl();

        this.headers = new HttpHeaders();
        this.headers.setBasicAuth(
                tabdemConfig.getApi().getUsername(),
                tabdemConfig.getApi().getPassword());
        this.headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public TandemMessage callTandemApi(TandemMessage tandemMessage) {

        HttpEntity<TandemMessage> request = new HttpEntity<>(
                tandemMessage,
                headers);

        try {
            ResponseEntity<TandemMessage> response = restTemplate.exchange(
                    url,
                HttpMethod.POST,
                request,
                TandemMessage.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            TandemMessage responseBody = response.getBody();
                logger.info("Tandem Service API 回應成功: {}", responseBody);
                return responseBody;
            } else {
                logger.error("Tandem Service API 呼叫失敗: {}", response.getStatusCode());
            throw new TandemApiException(
                    "Tandem Service API 回應非 200 狀態碼: " + response.getStatusCode(),
                    ReturnCode.TANDEM_HTTP_ERROR.getCode());
        } 

        } catch (RestClientException e) {
            logger.error("Tandem Service API 呼叫發生錯誤", e);
            throw new TandemApiException(
                    "Tandem Service API 呼叫發生錯誤: " + e.getMessage(),
                    ReturnCode.TANDEM_REQUEST_ERROR.getCode(),
                    e);
        }
    }
}
