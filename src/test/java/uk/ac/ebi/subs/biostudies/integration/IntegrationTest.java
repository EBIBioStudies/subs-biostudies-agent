package uk.ac.ebi.subs.biostudies.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.BioStudiesAgentApp;
import uk.ac.ebi.subs.biostudies.BioStudiesApiDependentTest;
import uk.ac.ebi.subs.biostudies.agent.ProjectsProcessor;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Project;
import uk.ac.ebi.subs.processing.ProcessingCertificate;

@RunWith(SpringRunner.class)
@Category(BioStudiesApiDependentTest.class)
@SpringBootTest(classes={BioStudiesAgentApp.class})
public class IntegrationTest {
    private Project project;
    private DataOwner dataOwner;
    @Autowired private ProjectsProcessor projectsProcessor;

    @Before
    public void buildUp() {
        dataOwner = DataOwner.builder()
            .email("test@example.com")
            .name("John Doe")
            .teamName("subs.testTeam")
            .build();

        Attribute attached = new Attribute();
        attached.setValue("I-DONT-EXIST");

        project = new Project();
        project.setAlias("pr1");
        project.setAccession("S-SUBST1");
        project.setTitle("a short title");
        project.setDescription("a short description");
        project.addAttribute("Project", attached);
        project.setTeam(Team.build(dataOwner.getTeamName()));
        project.setReleaseDate(LocalDate.MIN);
    }

    @Test
    public void processingErrorTest() {
        ProcessingCertificate cert = projectsProcessor.processProjects(dataOwner, project);

        assertNotNull(cert);
        assertNotNull(cert.getMessage());
        assertEquals(cert.getProcessingStatus(), ProcessingStatusEnum.Error);
    }
}
