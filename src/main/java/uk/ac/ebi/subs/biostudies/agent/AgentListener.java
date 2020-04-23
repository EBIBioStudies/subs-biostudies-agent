package uk.ac.ebi.subs.biostudies.agent;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.biostudies.converters.UsiSubmissionToDataOwner;
import uk.ac.ebi.subs.biostudies.interchange.QueueConfig;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.submittable.Project;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.processing.AccessionIdEnvelope;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.ProcessingCertificateEnvelope;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;

import java.util.Collections;
import java.util.List;

/**
 * This service is responsible for sending the submission's project(s) to BioStudies archive
 * and gets the list of processing certificates from the archive.
 */
@Service
@RequiredArgsConstructor
public class AgentListener {
    private static final Logger logger = LoggerFactory.getLogger(AgentListener.class);

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @NonNull
    private ProjectsProcessor projectsProcessor;
    @NonNull
    private UsiSubmissionToDataOwner usiSubmissionToDataOwner;

    @RabbitListener(queues = Queues.BIOSTUDIES_AGENT)
    public void handleProjectSubmission(SubmissionEnvelope submissionEnvelope) {
        Submission submission = submissionEnvelope.getSubmission();

        logger.info("Received submission {}", submission.getId());

        DataOwner dataOwner = usiSubmissionToDataOwner.convert(submission);
        Project project = submissionEnvelope.getProject();

        List<ProcessingCertificate> certificatesCompleted =
            Collections.singletonList(projectsProcessor.processProjects(dataOwner, project));

        ProcessingCertificateEnvelope certificateEnvelopeCompleted =
            new ProcessingCertificateEnvelope(
                submission.getId(), certificatesCompleted, submissionEnvelope.getJWTToken());

        logger.info(
            "Processed submission {} producing {} certificates", submission.getId(), certificatesCompleted.size());

        rabbitMessagingTemplate.convertAndSend(
            Exchanges.SUBMISSIONS, Topics.EVENT_SUBMISSION_AGENT_RESULTS, certificateEnvelopeCompleted);
    }

    @RabbitListener(queues = QueueConfig.USI_ARCHIVE_ACCESSIONIDS_PUBLISHED__QUEUE)
    public void fetchAccessionUpdateMessage(AccessionIdEnvelope envelope) {
        logger.debug("Received accession update message {}, {}",
                envelope.getBioStudiesAccessionId(), envelope.getBioSamplesAccessionIds());

        String submissionId = envelope.getBioStudiesAccessionId();
        List<String> sampleId = envelope.getBioSamplesAccessionIds();

        projectsProcessor.processUpdate(submissionId, sampleId);
    }
}
