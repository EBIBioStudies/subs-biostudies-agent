package uk.ac.ebi.subs.biostudies.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This value object contains the configuration for the BioStudies server.
 */
@Data
@ConfigurationProperties("usi.biostudies")
@Component
public class BioStudiesConfig {
    private String server;
    private Auth auth = new Auth();

    /**
     * This value class contains the authentication details of the BioStudies server.
     */
    @Data
    public static class Auth {
        private String login;
        private String password;
    }
}
