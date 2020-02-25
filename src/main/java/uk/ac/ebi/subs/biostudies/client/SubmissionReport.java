package uk.ac.ebi.subs.biostudies.client;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;

/**
 * This class is processing the response of the BioStudies server, when the user sends the list of projects to store.
 * It will parse the response to get the accession ID from BioStudies.
 */
@Data
public class SubmissionReport {
    private LogNode log;
    private String status;
    private BioStudiesSubmission submission;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BioStudiesSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(BioStudiesSubmission submission) {
        this.submission = submission;
    }

    private static final String SUBMISSION_ACCESSIONED_MESSAGE_PREFIX = "Submission generated accNo: ";
    private static final String SUBMISSION_ACCESSION_PROVIDED_MESSAGE_PREFIX = "Submission accession no: ";

    public String findAccession() {
        if (log == null) {
            return null;
        }

        String accession = findGeneratedAccession();
        if (accession == null) {
            accession = findProvidedAccession();
        }
        /*
         * Starting with "!{" indicates that it's an accession template, not an accession
         */
        if (accession != null && accession.startsWith("!{")){
            accession = null;
        }

        return accession;
    }

    private String findAccessionUsingPrefix(String messagePrefix){
        Optional<String> optionalString = log.nodeStream()
                .filter(node -> "INFO".equals(node.getLevel()))
                .map(node -> node.getMessage())
                .filter(message ->
                        message.startsWith(messagePrefix)
                )
                .findAny();

        if (!optionalString.isPresent()) {
            return null;
        }

        String accessionMessage = optionalString.get();

        String accession = accessionMessage.replace(messagePrefix, "").trim();
        return accession;
    }

    private String findGeneratedAccession() {
        return findAccessionUsingPrefix(SUBMISSION_ACCESSIONED_MESSAGE_PREFIX);
    }

    private String findProvidedAccession(){
        return findAccessionUsingPrefix(SUBMISSION_ACCESSION_PROVIDED_MESSAGE_PREFIX);
    }

    public Collection<String> findMessages(String level){
        return log.nodeStream()
                .filter(node -> level.equals(node.getLevel()))
                .map(node -> node.getMessage())
                .collect(Collectors.toList());
    }

    @Data
    public static class LogNode {
        private String level;
        private String message;
        private List<LogNode> subnodes = new ArrayList<>();

        public Stream<LogNode> nodeStream() {
            if (this.subnodes == null) {
                return Stream.of(this);
            }

            return Stream.concat(
                    Stream.of(this),
                    this.subnodes.stream().flatMap(LogNode::nodeStream)
            );
        }
    }
}
