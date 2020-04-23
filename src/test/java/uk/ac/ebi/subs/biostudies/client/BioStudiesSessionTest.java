package uk.ac.ebi.subs.biostudies.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.BioStudiesApiDependentTest;
import uk.ac.ebi.subs.biostudies.util.TestUtil;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    BioStudiesConfig.class,
    BioStudiesClient.class,
    BioStudiesClientTestContextConfiguration.class
})
@Category(BioStudiesApiDependentTest.class)
public class BioStudiesSessionTest {
    private static final String SUB_FILE = "BioStudiesSubmission.json";

    @Autowired private BioStudiesClient client;
    private DataOwner superUser;
    private BioStudiesSession session;
    private BioStudiesSubmission bioStudiesSubmission;

    @Before
    public void setUp() {
        session = client.getBioStudiesSession();
        bioStudiesSubmission = (BioStudiesSubmission) TestUtil.loadObjectFromJson(SUB_FILE, BioStudiesSubmission.class);
        superUser = DataOwner
            .builder()
            .email("admin_user@ebi.ac.uk")
            .name("Admin User")
            .build();
    }

    @Test
    public void createSubmission() {
        DataOwner regularUser = DataOwner
            .builder()
            .email("test@example.com")
            .name("John Doe")
            .teamName("subs.api-tester-team-1")
            .build();

        BioStudiesSubmission response = session.store(regularUser, bioStudiesSubmission);

        assertTrue(response.getAccno().startsWith("S-SUBS"));
    }

    @Test
    public void updateSubmission() {
        String expectedAccNo = "SUBSPRJ6";
        bioStudiesSubmission.setAccno(expectedAccNo);

        BioStudiesSubmission createResponse = session.store(superUser, bioStudiesSubmission);
        assertEquals(createResponse.getAccno(), expectedAccNo);

        String newTitle = "UPDATED Title";
        getSubmissionTitle(bioStudiesSubmission).setValue(newTitle);

        BioStudiesSubmission updateResponse = session.update(bioStudiesSubmission);
        assertEquals(updateResponse.getAccno(), expectedAccNo);
        assertEquals(getSubmissionTitle(updateResponse).getValue(), newTitle);
    }

    @Test
    public void getSubmission() {
        String expectedAccNo = "SUBSPRJ7";
        bioStudiesSubmission.setAccno(expectedAccNo);

        BioStudiesSubmission createResponse = session.store(superUser, bioStudiesSubmission);
        assertEquals(createResponse.getAccno(), expectedAccNo);

        BioStudiesSubmission requestedSubmission = session.getSubmission(expectedAccNo);
        assertEquals(requestedSubmission.getAccno(), expectedAccNo);
    }

    private BioStudiesAttribute getSubmissionTitle(BioStudiesSubmission submission) {
        return submission.getAttributes()
            .stream()
            .filter(attr -> attr.getName().equals("Title"))
            .findFirst()
            .orElse(null);
    }
}
