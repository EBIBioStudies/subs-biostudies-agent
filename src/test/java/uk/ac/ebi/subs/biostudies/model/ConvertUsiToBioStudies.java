package uk.ac.ebi.subs.biostudies.model;

import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.subs.biostudies.util.TestUtil.loadObjectFromJson;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.client.BioStudiesConfig;
import uk.ac.ebi.subs.biostudies.converters.UsiContactsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiFundingsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSection;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSubmission;
import uk.ac.ebi.subs.biostudies.converters.UsiPublicationsToBsSubsections;
import uk.ac.ebi.subs.data.submittable.Project;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    BioStudiesConfig.class,
    UsiProjectToBsSubmission.class,
    UsiProjectToBsSection.class,
    UsiPublicationsToBsSubsections.class,
    UsiContactsToBsSubSections.class,
    UsiFundingsToBsSubSections.class
})
@EnableAutoConfiguration
public class ConvertUsiToBioStudies {
    @Autowired
    private UsiProjectToBsSubmission usiProjectToBsSubmission;

    @Test
    public void testWithProject() {
        Project dspProject = (Project) loadObjectFromJson("DSPProject.json", Project.class);
        BioStudiesSubmission expected =
            (BioStudiesSubmission) loadObjectFromJson("BioStudiesSubmission.json", BioStudiesSubmission.class);
        BioStudiesSubmission actual = usiProjectToBsSubmission.convert(dspProject);

        compareSubmissions(expected, actual);
    }

    @Test
    public void testWithoutProject() {
        Project hcaProject = (Project) loadObjectFromJson("HCAProject.json", Project.class);
        BioStudiesSubmission expected =
            (BioStudiesSubmission) loadObjectFromJson("BioStudiesHCASubmission.json", BioStudiesSubmission.class);
        BioStudiesSubmission actual = usiProjectToBsSubmission.convert(hcaProject);

        compareSubmissions(expected, actual);
    }

    private void compareSubmissions(BioStudiesSubmission expected, BioStudiesSubmission actual) {
        assertEquals(expected, actual);

        compareRootSection(expected, actual);
        compareSubsections(expected, actual);

        compareSpecificSubsections("Author", expected, actual);
        compareSpecificSubsections("Publication", expected, actual);
        compareSpecificSubsections("Organisation", expected, actual);
    }

    private void compareRootSection(BioStudiesSubmission expected, BioStudiesSubmission actual) {
        assertEquals(expected.getAccno(), actual.getAccno());
        assertEquals(expected.getAttributes(), actual.getAttributes());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSection(), actual.getSection());
    }

    private void compareSubsections(BioStudiesSubmission expected, BioStudiesSubmission actual) {
        assertEquals(expected.getSection().getSubsections(), actual.getSection().getSubsections());

        List<BioStudiesSubsection> actualSubsections = actual.getSection().getSubsections();
        List<BioStudiesSubsection> expectedSubsections = expected.getSection().getSubsections();

        for (int idx = 0; idx < actualSubsections.size(); idx++) {
            assertEquals(expectedSubsections.get(idx), actualSubsections.get(idx));
        }
    }

    private void compareSpecificSubsections(String type, BioStudiesSubmission expected, BioStudiesSubmission actual) {
        assertEquals(fetchSubsection(expected, type), fetchSubsection(actual, type));
    }

    private List<BioStudiesSubsection> fetchSubsection(BioStudiesSubmission submission, String type) {
        return submission
            .getSection()
            .getSubsections()
            .stream()
            .filter(subsection -> type.equals(subsection.getType()))
            .collect(Collectors.toList());
    }
}
