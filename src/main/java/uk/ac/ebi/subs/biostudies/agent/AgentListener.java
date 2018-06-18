package uk.ac.ebi.subs.biostudies.agent;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.biostudies.converters.UsiSubmissionToDataOwner;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.submittable.Project;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.ProcessingCertificateEnvelope;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;

import java.util.List;

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
        List < Project > projects = submissionEnvelope.getProjects();

        List<ProcessingCertificate> certificatesCompleted = projectsProcessor.processProjects(dataOwner,projects);

        ProcessingCertificateEnvelope certificateEnvelopeCompleted = new ProcessingCertificateEnvelope(
                submission.getId(),
                certificatesCompleted
        );
        logger.info("Processed submission {} producing {} certificates",
                submission.getId(),
                certificatesCompleted.size()
        );

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, Topics.EVENT_SUBMISSION_AGENT_RESULTS, certificateEnvelopeCompleted);
    }

}