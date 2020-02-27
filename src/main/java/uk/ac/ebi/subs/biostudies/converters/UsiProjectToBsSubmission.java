package uk.ac.ebi.subs.biostudies.converters;

import static uk.ac.ebi.subs.biostudies.converters.ConverterUtils.addBioStudiesAttributeIfNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.biostudies.client.BioStudiesConfig;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubsection;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.submittable.Project;

/**
 * This component responsible for converting USI {@link Project} entity
 * to BioStudies {@link BioStudiesSubmission} entity.
 */
@Component
@Data
public class UsiProjectToBsSubmission implements Converter<Project,BioStudiesSubmission> {
    private static final String PROJECT_ATTR = "Project";
    private static final String ATTACH_TO_ATTR = "AttachTo";
    private static final String SECTION_INTERNAL_ACCESSION = "PROJECT";
    private static final String SUBSECTION_INTERNAL_ACCESSION_PREFIX = "SECT";

    @NonNull
    private UsiProjectToBsSection usiProjectToBsSection;

    @NonNull
    private final BioStudiesConfig bioStudiesConfig;

    @Override
    public BioStudiesSubmission convert(Project source) {
        BioStudiesSubmission submission = new BioStudiesSubmission();

        submission.getAttributes().addAll(convertAttributes(source));
        addBioStudiesAttributeIfNotNull(submission.getAttributes(), "Title", source.getTitle());
        addBioStudiesAttributeIfNotNull(submission.getAttributes(), "ReleaseDate", source.getReleaseDate().toString());
        addBioStudiesAttributeIfNotNull(submission.getAttributes(), "DataSource", "USI");

        submission.setType("submission");
        submission.setSection(usiProjectToBsSection.convert(source));

        int sectCounter = 0;
        submission.getSection().setAccno(SECTION_INTERNAL_ACCESSION);

        for (BioStudiesSubsection subsection : submission.getSection().getSubsections()) {
            if (!subsection.isAccessioned()) {
                sectCounter++;
                subsection.setAccno(SUBSECTION_INTERNAL_ACCESSION_PREFIX + sectCounter);
            }
        }

        return submission;
    }

    private List<BioStudiesAttribute> convertAttributes(Project project) {
        List<BioStudiesAttribute> bioStudiesAttributes = new ArrayList<>();

        for (Map.Entry<String, Collection<Attribute>> attributes : project.getAttributes().entrySet()) {
            for (Attribute attribute : attributes.getValue()) {
                String attrName = attributes.getKey().equals(PROJECT_ATTR) ? ATTACH_TO_ATTR : attributes.getKey();
                bioStudiesAttributes.add(
                    BioStudiesAttribute.builder().name(attrName).value(attribute.getValue()).build());
            }

        }

        return bioStudiesAttributes;
    }
}
