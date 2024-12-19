package tw.com.sc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 由於不要直接使用 Spring Boot 的 Actuator，且只提供給負載平衡器使用，
 * 因此在此實作自定義的健康檢查端點。
 */
@RestController
public class SystemHealthController {

    private static final Logger logger = LoggerFactory.getLogger(SystemHealthController.class);

    
    private final HealthEndpoint healthEndpoint;

    public SystemHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    /**
     * Actuator 的檢查端點，不要直接對外部IP開放。
     * 所以在這裡提供 health 端點供負載平衡器使用。
     *
     * @return 健康狀態的文字描述
     */
    @GetMapping(value = "/health", produces = "text/plain")
    public ResponseEntity<String> health() {
        HealthComponent health = healthEndpoint.health();
        Status status = health.getStatus();

        if (status == Status.UP) {
            return ResponseEntity.ok("UP");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("DOWN");
        }
    }
}
