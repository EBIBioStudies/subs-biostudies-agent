package uk.ac.ebi.subs.biostudies.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.web.util.UriUtils;
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
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("sse", "true"); //enables super user actions
        parameters.put("onBehalf", dataOwner.getEmail());
        parameters.put("name", dataOwner.getName());

        List<String> params = parameters.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + UriUtils.encodeQueryParam(entry.getValue(), "UTF-8"))
            .collect(Collectors.toList());

        String queryString = "?" + String.join("&", params);

        return URI.create(bioStudiesConfig.getServer() + "/submissions" + queryString);
    }

    private HttpEntity<String> createBioStudiesSubmitRequest(String sessionId, BioStudiesSubmission submission) {
        ObjectMapper objectMapper = new ObjectMapper();
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

    private void logHttpError(HttpStatusCodeException e, String httpErrorTitleMessage) {
        logger.error(httpErrorTitleMessage);
        logger.error("Response code: {}", e.getRawStatusCode());
        logger.error("Response body: {}", e.getResponseBodyAsString());
    }

    private void logSubmissionResponse(HttpEntity<BioStudiesSubmission> response) {
        ObjectMapper om = new ObjectMapper();
        String submissionReport = null;
        try {
            submissionReport = om.writeValueAsString(response.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        logger.info("submission response:");
        logger.info(submissionReport);
    }

    private void logSubmission(BioStudiesSubmission submission) {
        ObjectMapper om = new ObjectMapper();
        try {
            String jsonSubmission = om.writeValueAsString(submission);
            logger.info("Submission as json:");
            logger.info(jsonSubmission);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}