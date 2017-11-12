package fileuploader.configuration;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Created by luisoliveira on 11/15/17.
 */
@Component
public class HealthCheck implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().build();
    }

}
