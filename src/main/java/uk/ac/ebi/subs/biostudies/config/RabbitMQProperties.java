package uk.ac.ebi.subs.biostudies.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("spring.rabbitmq")
@Component
public class RabbitMQProperties {
    private String host;
    private String username;
    private String password;
    private String virtualHost;
}
