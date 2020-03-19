package uk.ac.ebi.subs.biostudies.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;

/**
 * This class responsible to send new BioStudies submissions to BioStudies server.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class BioStudiesSession {
    private static final String SESSION_ID_HEADER = "X-Session-Token";
    private static final String SUBMISSION_TYPE_HEADER = "Submission_Type";
    private static final String SUBMISSION_TYPE_PARAM = "application/json";
    private static final Logger logger = LoggerFactory.getLogger(BioStudiesSession.class);

    @NonNull private final RestTemplate restTemplate;
    @NonNull private final BioStudiesConfig bioStudiesConfig;
    @NonNull private final BioStudiesLoginResponse bioStudiesLoginResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BioStudiesSubmission store(DataOwner dataOwner, BioStudiesSubmission bioStudiesSubmission) {
        logSubmission(bioStudiesSubmission);
        HttpEntity<BioStudiesSubmission> response;

        try {
            response = restTemplate.postForEntity(
                getRequestUri(dataOwner),
                createBioStudiesSubmitRequest(bioStudiesLoginResponse.getSessid(), bioStudiesSubmission),
                BioStudiesSubmission.class);
        } catch (HttpServerErrorException | HttpClientErrorException httpError) {
            logHttpError(httpError, "Http server error during create/update");
            throw httpError;
        }

        logSubmissionResponse(response);

        return response.getBody();
    }

    public BioStudiesSubmission update(BioStudiesSubmission submission) {
        return restTemplate.postForObject(
            bioStudiesConfig.getServer() + "/submissions",
            createBioStudiesSubmitRequest(bioStudiesLoginResponse.getSessid(), submission),
            BioStudiesSubmission.class);
    }

    public BioStudiesSubmission getSubmission(String accNo) {
        return restTemplate.exchange(
            bioStudiesConfig.getServer() + "/submissions/" + accNo + ".json",
            HttpMethod.GET,
            new HttpEntity(getSecuredHeaders(bioStudiesLoginResponse.getSessid())),
            BioStudiesSubmission.class).getBody();
    }

    private URI getRequestUri(DataOwner dataOwner) {
        return UriComponentsBuilder
                .fromHttpUrl(bioStudiesConfig.getServer() + "/submissions")
                .queryParam("sse", "true")
                .queryParam("onBehalf", dataOwner.getEmail())
                .queryParam("name", dataOwner.getName())
                .build()
                .toUri();
    }

    private HttpEntity<String> createBioStudiesSubmitRequest(String sessionId, BioStudiesSubmission submission) {
        HttpHeaders headers = getSecuredHeaders(sessionId);
        headers.add(SUBMISSION_TYPE_HEADER, SUBMISSION_TYPE_PARAM);

        try {
            return new HttpEntity<>(objectMapper.writeValueAsString(submission), headers);
        } catch (JsonProcessingException jsonError) {
            HttpServerErrorException httpError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            logHttpError(httpError, "Error in submission serialization");
            throw httpError;
        }
    }

    private HttpHeaders getSecuredHeaders(String sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(SESSION_ID_HEADER, sessionId);

        return headers;
    }

    private void logHttpError(HttpStatusCodeException exception, String httpErrorTitleMessage) {
        logger.error(httpErrorTitleMessage);
        logger.error("Response code: {}", exception.getRawStatusCode());
        logger.error("Response body: {}", exception.getResponseBodyAsString());
    }

    private void logSubmissionResponse(HttpEntity<BioStudiesSubmission> response) {
        String submissionReport = null;

        try {
            submissionReport = objectMapper.writeValueAsString(response.getBody());
        } catch (JsonProcessingException jsonException) {
            jsonException.printStackTrace();
        }

        logger.info("submission response:");
        logger.info(submissionReport);
    }

    private void logSubmission(BioStudiesSubmission submission) {
        try {
            String jsonSubmission = objectMapper.writeValueAsString(submission);
            logger.info("Submission as json:");
            logger.info(jsonSubmission);
        } catch (JsonProcessingException jsonException) {
            jsonException.printStackTrace();
        }
    }
}
