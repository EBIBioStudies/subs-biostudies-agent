package uk.ac.ebi.subs.biostudies.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmissionWrapper;
import uk.ac.ebi.subs.biostudies.model.DataOwner;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class responsible to send new BioStudies submissions to BioStudies server.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class BioStudiesSession {
    private static final Logger logger = LoggerFactory.getLogger(BioStudiesSession.class);

    @NonNull
    private final BioStudiesLoginResponse bioStudiesLoginResponse;
    @NonNull
    private final BioStudiesConfig bioStudiesConfig;
    @NonNull
    private final RestTemplate restTemplate;

    private static final String SESSION_PARAM_NAME = "X-Session-Token";

    // TODO update the client to run with new submitter
    // TODO run a local instance and point there
    public SubmissionReport store(DataOwner dataOwner, BioStudiesSubmission bioStudiesSubmission) {
        BioStudiesSubmissionWrapper wrapper = new BioStudiesSubmissionWrapper();
        wrapper.getSubmissions().add(bioStudiesSubmission);

        logSubmission(wrapper);

        HttpEntity<SubmissionReport> response;
        try {
            URI url = commandUri(dataOwner);
            HttpHeaders headers = new HttpHeaders();
            headers.add(SESSION_PARAM_NAME, bioStudiesLoginResponse.getSessid());
            HttpEntity<BioStudiesSubmissionWrapper> request = new HttpEntity<>(wrapper, headers);
            response = restTemplate.postForEntity(
                    url,
                    wrapper,
                    SubmissionReport.class
            );
        } catch (HttpServerErrorException e) {
            logHttpError(e, "Http server error during createupdate");
            throw e;
        } catch (HttpClientErrorException e) {
            logHttpError(e, "Http client error during createupdate");
            throw e;
        }

        logSubmissionResponse(response);

        return response.getBody();
    }

    public SubmissionReport update(BioStudiesSubmission submission) {
        BioStudiesSubmissionWrapper wrapper = new BioStudiesSubmissionWrapper();
        wrapper.getSubmissions().add(submission);
        return restTemplate.postForObject(
                bioStudiesConfig.getServer() + "/submit/createupdate" + "?BIOSTDSESS=" + bioStudiesLoginResponse
                        .getSessid(),
                wrapper,
                SubmissionReport.class
        );
    }

    public BioStudiesSubmission getSubmission(String accNo) {
        return restTemplate.getForObject(
                bioStudiesConfig.getServer() + "/submission/" + accNo + "?BIOSTDSESS=" + bioStudiesLoginResponse
                        .getSessid(),
                BioStudiesSubmission.class);
    }

    private void logHttpError(HttpStatusCodeException e, String httpErrorTitleMessage) {
        logger.error(httpErrorTitleMessage);
        logger.error("Response code: {}", e.getRawStatusCode());
        logger.error("Response body: {}", e.getResponseBodyAsString());
    }

    private void logSubmissionResponse(HttpEntity<SubmissionReport> response) {
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

    private void logSubmission(BioStudiesSubmissionWrapper wrapper) {
        ObjectMapper om = new ObjectMapper();
        try {
            String jsonSubmission = om.writeValueAsString(wrapper);
            logger.info("Submission as json:");
            logger.info(jsonSubmission);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private URI commandUri(DataOwner dataOwner) {
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
}