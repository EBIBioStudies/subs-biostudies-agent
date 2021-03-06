package uk.ac.ebi.subs.biostudies.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;

/**
 * This class represent a client for the BioSamples REST interface.
 */
@Component
@RequiredArgsConstructor
public class BioStudiesClient {
    private static final long FIVE_MINUTES_IN_MILLIS = 300000;
    private static final Logger logger = LoggerFactory.getLogger(BioStudiesClient.class);

    @NonNull
    private final BioStudiesConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    private Long sessionExpiryTime;
    private BioStudiesSession cachedSession;

    public BioStudiesSession getBioStudiesSession(){
        long currentTime = System.currentTimeMillis();
        boolean sessionExpired = (sessionExpiryTime != null &&  currentTime >= sessionExpiryTime);

        if (cachedSession == null || sessionExpired){
            cacheSession();
        }

        return this.cachedSession;
    }

    @Synchronized
    private void cacheSession() {
        BioStudiesSession session = initialiseSession();
        Long expiryTime = null;

        String token = session.getBioStudiesLoginResponse().getSessid();

        DecodedJWT jwt = JWT.decode(token);
        if (jwt.getExpiresAt() != null ){
            Date expiryDate = jwt.getExpiresAt();
            expiryTime = expiryDate.getTime() - FIVE_MINUTES_IN_MILLIS;
        }

        this.cachedSession = session;
        this.sessionExpiryTime = expiryTime;
    }

    private BioStudiesSession initialiseSession() {
        BioStudiesLoginResponse loginResponse ;

        try {
            loginResponse = restTemplate.postForObject(loginUri(), config.getAuth(), BioStudiesLoginResponse.class);
        }
        catch (HttpClientErrorException httpError){
            if (httpError.getStatusCode().equals(HttpStatus.UNAUTHORIZED)){
                throw new IllegalArgumentException("Login failed, check username and password");
            }

            logger.error("Http error during login");
            logger.error("Response code: {}", httpError.getRawStatusCode());
            logger.error("Response body: {}", httpError.getResponseBodyAsString());

            throw httpError;
        }

        if (loginResponse.getSessid() == null){
            throw new IllegalStateException("Session id not found: " + loginResponse);
        }

        return BioStudiesSession.of(restTemplate, config, loginResponse);
    }

    private URI loginUri() {
        return URI.create(config.getServer() + "/auth/login");
    }
}
