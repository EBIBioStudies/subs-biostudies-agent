package uk.ac.ebi.subs.biostudies.agent;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClient;
import uk.ac.ebi.subs.biostudies.client.BioStudiesSession;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSubmission;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesLink;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Project;
import uk.ac.ebi.subs.processing.ProcessingCertificate;

/**
 * This service sends a list of projects (related to the submitted submission) to the BioStudies archive.
 */
@Service
@RequiredArgsConstructor
public class ProjectsProcessor {
    @NonNull private final BioStudiesClient bioStudiesClient;
    @NonNull private UsiProjectToBsSubmission converter;

    public static final String PROJECT_PROCESSING_HAS_FAILED_MESSAGE = "Project processing has failed: %s";

    public ProcessingCertificate processProjects(DataOwner dataOwner, Project project) {
        ProcessingCertificate processingCertificate;
        try {
            BioStudiesSession bioStudiesSession = bioStudiesClient.getBioStudiesSession();
            processingCertificate = processProject(dataOwner, project, bioStudiesSession);
        } catch (IllegalStateException ise) {
            processingCertificate = createProcessingCertificate(project, ProcessingStatusEnum.Error);
            processingCertificate.setMessage(String.format(PROJECT_PROCESSING_HAS_FAILED_MESSAGE, ise.getMessage()));
        } catch (HttpStatusCodeException hsce) {
            processingCertificate = createProcessingCertificate(project, ProcessingStatusEnum.Error);
            processingCertificate.setMessage(
                    String.format(PROJECT_PROCESSING_HAS_FAILED_MESSAGE, hsce.getStatusText()));
        }

        return processingCertificate;
    }

    private ProcessingCertificate createProcessingCertificate(Project project, ProcessingStatusEnum processingStatus) {
        return new ProcessingCertificate(project, Archive.BioStudies, processingStatus);
    }

    private ProcessingCertificate processProject(DataOwner dataOwner, Project project, BioStudiesSession bioStudiesSession) {
        BioStudiesSubmission bioStudiesSubmission = converter.convert(project);

        //TODO has this alias+team combo been used already
        if (project.isAccessioned()) {
            bioStudiesSubmission.setAccno(project.getAccession());
        }

        BioStudiesSubmission submission = bioStudiesSession.store(dataOwner, bioStudiesSubmission);
        ProcessingCertificate cert = createProcessingCertificate(project, ProcessingStatusEnum.Completed);
        String accession = submission.getAccno();

        if (accession != null){
            cert.setAccession(accession);
        }

        return cert;
    }

    public void processUpdate(String submissionId, List<String> samples) {
        BioStudiesSession session = bioStudiesClient.getBioStudiesSession();
        BioStudiesSubmission submission = session.getSubmission(submissionId);
        submission.getSection().setLinks(samples.stream().map(this::createLink).collect(toList()));
        session.update(submission);
    }

    private BioStudiesLink createLink(String sampleId){
        BioStudiesLink link = new BioStudiesLink();
        link.setAttributes(singletonList(BioStudiesAttribute.builder().name("Type").value("BioSample").build()));
        link.setUrl(sampleId);
        return link;
    }
}
