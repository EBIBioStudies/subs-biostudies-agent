package uk.ac.ebi.subs.biostudies.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.ac.ebi.subs.biostudies.util.TestUtil.loadObjectFromJson;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.BioStudiesApiDependentTest;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClient;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClientTestContextConfiguration;
import uk.ac.ebi.subs.biostudies.client.BioStudiesConfig;
import uk.ac.ebi.subs.biostudies.client.BioStudiesSession;
import uk.ac.ebi.subs.biostudies.converters.UsiContactsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiFundingsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSection;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSubmission;
import uk.ac.ebi.subs.biostudies.converters.UsiPublicationsToBsSubsections;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSection;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;
import uk.ac.ebi.subs.data.submittable.Project;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    BioStudiesConfig.class,
    BioStudiesClient.class,
    BioStudiesClientTestContextConfiguration.class,
    UsiProjectToBsSubmission.class,
    UsiProjectToBsSection.class,
    UsiPublicationsToBsSubsections.class,
    UsiContactsToBsSubSections.class,
    UsiFundingsToBsSubSections.class
})
@Category(BioStudiesApiDependentTest.class)
public class DSPToBioStudiesSubmissionTest {
    private DataOwner user;

    private BioStudiesSession session;

    @Autowired
    private BioStudiesClient client;

    @Autowired
    private UsiProjectToBsSubmission usiProjectToBsSubmission;

    @Before
    public void setUp() {
        session = client.getBioStudiesSession();
        user = DataOwner
            .builder()
            .email("admin_user@ebi.ac.uk")
            .name("Admin User")
            .build();
    }

    @Test
    public void submit() {
        Project dspProject = (Project) loadObjectFromJson("TestProject.json", Project.class);
        BioStudiesSubmission bioStudiesSubmission = usiProjectToBsSubmission.convert(dspProject);
        BioStudiesSubmission response = session.store(user, bioStudiesSubmission);

        assertSubmission(response);
        assertRootSection(response.getSection());
    }

    private void assertSubmission(BioStudiesSubmission submission) {
        assertThat(submission.getAccno(), startsWith("S-SUBS"));

        assertThat(submission.getAttributes(), hasSize(4));
        assertAttribute(submission.getAttributes().get(0), "DataSource", "USI");
        assertAttribute(submission.getAttributes().get(1), "Title", "Test Title");
        assertAttribute(submission.getAttributes().get(2), "ReleaseDate", "2030-12-31");
        assertAttribute(submission.getAttributes().get(3), "AttachTo", "DSP");
    }

    private void assertRootSection(BioStudiesSection rootSection) {
        assertEquals(rootSection.getType(), "Study");

        assertThat(rootSection.getAttributes(), hasSize(2));
        assertAttribute(rootSection.getAttributes().get(0), "Description", "Test description");
        assertAttribute(rootSection.getAttributes().get(1), "alias", "tp-1007");

        assertThat(rootSection.getLinks(), empty());
        assertThat(rootSection.getSubsections(), empty());
    }

    private void assertAttribute(BioStudiesAttribute attribute, String expectedName, String expectedValue) {
        assertEquals(attribute.getName(), expectedName);
        assertEquals(attribute.getValue(), expectedValue);
    }
}
