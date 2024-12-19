package tw.com.sc.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tw.com.sc.model.ClientSettings;

@Component
public class ClientValidator {

    private static final Logger logger = LoggerFactory.getLogger(ClientValidator.class);
    private Map<String, String> clientMap = new HashMap<>();

    public ClientValidator(ClientSettings clientSettings) {
        // init client map
        // {base64(clientId): base64(clientSecret)}
        clientSettings.getClients().forEach(client -> {
            clientMap.put(Base64.getEncoder().encodeToString(client.getId().getBytes()),
                    Base64.getEncoder().encodeToString(client.getSecret().getBytes()));
        });

    }

    public boolean validate(String clientId, String clientSecret) {
        if (!clientMap.containsKey(clientId)) {
            logger.error("Client ID 不正確: {}", clientId);
            return false;
        }

        if (!clientMap.get(clientId).equals(clientSecret)) {
            logger.error("Client Secret 不相符: {}", clientId);
            return false;
        }

        return true;
    }

}