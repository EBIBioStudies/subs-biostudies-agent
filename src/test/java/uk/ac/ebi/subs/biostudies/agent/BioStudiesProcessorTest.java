package uk.ac.ebi.subs.biostudies.agent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClient;
import uk.ac.ebi.subs.biostudies.client.BioStudiesSession;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSubmission;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Project;
import uk.ac.ebi.subs.processing.ProcessingCertificate;

@RunWith(SpringRunner.class)
public class BioStudiesProcessorTest {
    @Mock private BioStudiesClient bioStudiesClient;
    @Mock private BioStudiesSession bioStudiesSession;
    @Mock private UsiProjectToBsSubmission usiProjectToBsSubmission;
    @Mock private BioStudiesSubmission submission;

    private Project project;
    private DataOwner dataOwner;

    @InjectMocks
    @Spy
    ProjectsProcessor projectsProcessor;

    @Before
    public void buildUp() {
        project = new Project();
        project.setAlias("pr1");
        project.setTeam(Team.build("team"));
        project.setReleaseDate(LocalDate.MIN);

        dataOwner = DataOwner.builder()
                .email("test@example.com")
                .name("John Doe")
                .build();
    }

    @Test
    public void testCreateOfNewProject() {
        String accession = "SO1"; //totally unrepresentative accession style

        BioStudiesSubmission bioStudiesSubmission = new BioStudiesSubmission();

        when(submission.getAccno()).thenReturn(accession);
        when(usiProjectToBsSubmission.convert(project)).thenReturn(bioStudiesSubmission);
        when(bioStudiesClient.getBioStudiesSession()).thenReturn(bioStudiesSession);
        when(bioStudiesSession.store(dataOwner, bioStudiesSubmission)).thenReturn(submission);

        ProcessingCertificate expectedCert = new ProcessingCertificate(
            project,
            uk.ac.ebi.subs.data.component.Archive.BioStudies,
            ProcessingStatusEnum.Completed,
            accession
        );

        ProcessingCertificate actualCert = projectsProcessor.processProjects(dataOwner,project);

        assertEquals(expectedCert, actualCert);
    }

    @Test
    public void testUpdateOfExistingProject() {
        String accession = "SO1"; //totally unrepresentative accession style
        project.setAccession(accession);

        BioStudiesSubmission bioStudiesSubmission = new BioStudiesSubmission();

        when(submission.getAccno()).thenReturn(accession);
        when(usiProjectToBsSubmission.convert(project)).thenReturn(bioStudiesSubmission);
        when(bioStudiesClient.getBioStudiesSession()).thenReturn(bioStudiesSession);
        when(bioStudiesSession.store(dataOwner, bioStudiesSubmission)).thenReturn(submission);

        ProcessingCertificate expectedCert = new ProcessingCertificate(
            project,
            uk.ac.ebi.subs.data.component.Archive.BioStudies,
            ProcessingStatusEnum.Completed,
            accession
        );

        ProcessingCertificate actualCert = projectsProcessor.processProjects(dataOwner, project);

        assertEquals(expectedCert, actualCert);
    }

}
