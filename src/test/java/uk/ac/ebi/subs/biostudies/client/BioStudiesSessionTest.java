package uk.ac.ebi.subs.biostudies.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.BioStudiesApiDependentTest;
import uk.ac.ebi.subs.biostudies.TestUtil;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.DataOwner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        BioStudiesConfig.class,
        BioStudiesClient.class,
        BioStudiesClientTestContextConfiguration.class
})
@Category(BioStudiesApiDependentTest.class)
public class BioStudiesSessionTest {

    @Autowired
    private BioStudiesClient client;

    private BioStudiesSession session;

    private BioStudiesSubmission bioStudiesSubmission;

    private DataOwner dataOwner;

    @Autowired
    private BioStudiesConfig bioStudiesConfig;

    @Before
    public void buildup() {
        session = client.getBioStudiesSession();

        bioStudiesSubmission = (BioStudiesSubmission) TestUtil.loadObjectFromJson(
                "exampleProject_biostudies.json", BioStudiesSubmission.class
        );

        dataOwner = DataOwner.builder()
                .email("test@example.com")
                .name("John Doe")
                .teamName("subs.api-tester-team-1")
                .build();
    }

    @Test
    public void createGood() throws JsonProcessingException {
        SubmissionReport response = session.store(dataOwner,bioStudiesSubmission);

        assertEquals("OK", response.getStatus());
        assertNotNull(response.findAccession());
        assertTrue(response.findAccession().startsWith("S-SUBS"));

        System.out.println(response.findAccession());
    }

    @Test
    public void updateGood() {
        String expectedAccNo = "SUBSPRJ6";
        bioStudiesSubmission.setAccno(expectedAccNo);

        SubmissionReport response = session.store(dataOwner,bioStudiesSubmission);

        assertEquals("OK", response.getStatus());
        assertNotNull(response.findAccession());
        assertEquals(expectedAccNo,response.findAccession());

        System.out.println(response.findAccession());
    }
}
