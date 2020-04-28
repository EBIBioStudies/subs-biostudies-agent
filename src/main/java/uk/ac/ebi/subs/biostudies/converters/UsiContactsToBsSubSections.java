package uk.ac.ebi.subs.biostudies.converters;

import static uk.ac.ebi.subs.biostudies.converters.ConverterUtils.addBioStudiesAttributeIfNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubsection;
import uk.ac.ebi.subs.data.component.Contact;
import uk.ac.ebi.subs.data.component.Contacts;

/**
 * This component responsible for converting USI {@link Contact}s entiti(es)
 * to BioStudies {@link BioStudiesSubsection} entiti(es).
 */
@Component
@Data
public class UsiContactsToBsSubSections implements Converter<Contacts, List<BioStudiesSubsection>> {

    @Override
    public List<BioStudiesSubsection> convert(Contacts source) {
        Map<String, String> affiliationsToRefNames = affiliationRefs(source.getContacts());

        List<BioStudiesSubsection> subsections = source.getContacts().stream()
                .map(contact -> contactToSubsection(contact, affiliationsToRefNames))
                .collect(Collectors.toList());

        subsections.addAll(
                affiliationsToRefNames.entrySet().stream()
                        .map(this::affiliationRefToSubsection)
                        .collect(Collectors.toList())
        );

        return subsections;
    }

    private BioStudiesSubsection affiliationRefToSubsection(Map.Entry<String, String> affiliationRefName) {
        BioStudiesSubsection subsection = new BioStudiesSubsection();

        subsection.setType("Organisation");
        subsection.setAccno(affiliationRefName.getValue());
        subsection.getAttributes().add(
                BioStudiesAttribute.builder()
                        .name("Name")
                        .value(affiliationRefName.getKey())
                        .build());

        return subsection;
    }

    private BioStudiesSubsection contactToSubsection(Contact contact, Map<String, String> affiliationRefNames) {
        BioStudiesSubsection subsection = new BioStudiesSubsection();
        subsection.setType("Author");

        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "Name", contactName(contact));
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "firstName", contact.getFirstName());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "middleInitials", contact.getMiddleInitials());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "lastName", contact.getLastName());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "E-mail", contact.getEmail());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "address", contact.getAddress());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "phone", contact.getPhone());
        addBioStudiesAttributeIfNotNull(subsection.getAttributes(), "role", String.join(", ", contact.getRoles()));

        if (contact.getAffiliation() != null && affiliationRefNames.containsKey(contact.getAffiliation())) {
            String affiliationRef = affiliationRefNames.get(contact.getAffiliation());

            subsection.getAttributes().add(BioStudiesAttribute.builder().name("Organisation").value(affiliationRef).isReference(true).build());
        }

        return subsection;
    }

    private String contactName(Contact contact) {
        List<String> nameElements = new ArrayList<>();

        if (contact.getFirstName() != null) {
            nameElements.add(contact.getFirstName());
        }
        if (contact.getMiddleInitials() != null) {
            nameElements.add(contact.getMiddleInitials());
        }
        if (contact.getLastName() != null) {
            nameElements.add(contact.getLastName());
        }

        String name = String.join(" ", nameElements);
        if (name.isEmpty()) {
            return null;
        }

        return name;
    }

    private Map<String, String> affiliationRefs(List<Contact> contacts) {
        List<String> distinctAffiliations = contacts.stream()
                .map(Contact::getAffiliation)
                .filter(Objects::nonNull)
                .filter(affiliation -> !affiliation.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> affiliationRefs = new LinkedHashMap<>();

        int affiliationCounter = 1;

        for (String affiliation : distinctAffiliations) {
            String ref = "o" + affiliationCounter;
            affiliationCounter++;
            affiliationRefs.put(affiliation, ref);
        }

        return Collections.unmodifiableMap(affiliationRefs);
    }
}
