package uk.ac.ebi.subs.biostudies.integration;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.biostudies.agent.AgentListener;
import uk.ac.ebi.subs.biostudies.agent.ProjectsProcessor;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClient;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClientTestContextConfiguration;
import uk.ac.ebi.subs.biostudies.client.BioStudiesConfig;
import uk.ac.ebi.subs.biostudies.client.BioStudiesSession;
import uk.ac.ebi.subs.biostudies.config.RabbitMQProperties;
import uk.ac.ebi.subs.biostudies.converters.UsiContactsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiFundingsToBsSubSections;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSection;
import uk.ac.ebi.subs.biostudies.converters.UsiProjectToBsSubmission;
import uk.ac.ebi.subs.biostudies.converters.UsiPublicationsToBsSubsections;
import uk.ac.ebi.subs.biostudies.converters.UsiSubmissionToDataOwner;
import uk.ac.ebi.subs.biostudies.model.BioStudiesAttribute;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSection;
import uk.ac.ebi.subs.biostudies.model.BioStudiesSubmission;
import uk.ac.ebi.subs.biostudies.util.TestUtil;
import uk.ac.ebi.subs.messaging.Queues;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    AgentListener.class,
    BioStudiesConfig.class,
    BioStudiesClient.class,
    RabbitMQProperties.class,
    ProjectsProcessor.class,
    UsiProjectToBsSubmission.class,
    UsiProjectToBsSection.class,
    UsiPublicationsToBsSubsections.class,
    UsiContactsToBsSubSections.class,
    UsiFundingsToBsSubSections.class,
    UsiSubmissionToDataOwner.class,
    BioStudiesClientTestContextConfiguration.class
})
public class DSPQueueIntegrationTest {
    private BioStudiesSession session;

    @Autowired
    RabbitMQProperties rabbitMQProperties;

    @Autowired
    BioStudiesClient bioStudiesClient;

    @Before
    public void setUp() {
        session = bioStudiesClient.getBioStudiesSession();
    }

    @Test
    public void readFromAgentQueue()
    throws Exception {
        publishTestMessage();
        BioStudiesSubmission submission = waitForSubmissionProcessing();

        assertNotNull("The submission wasn't found", submission);
        assertSubmission(submission);
        assertRootSection(submission.getSection());
    }

    private void assertSubmission(BioStudiesSubmission submission) {
        assertEquals(submission.getAccno(), "S-SUBSTEST123");

        assertThat(submission.getAttributes(), hasSize(4));
        assertAttribute(submission.getAttributes().get(0), "DataSource", "USI");
        assertAttribute(submission.getAttributes().get(1), "Title", "BioStudies Agent iTest Submission");
        assertAttribute(submission.getAttributes().get(2), "ReleaseDate", "2018-09-21");
        assertAttribute(submission.getAttributes().get(3), "AttachTo", "DSP");
    }

    private void assertRootSection(BioStudiesSection rootSection) {
        assertEquals(rootSection.getType(), "Study");

        assertThat(rootSection.getAttributes(), hasSize(2));
        assertAttribute(rootSection.getAttributes().get(0), "Description", "A BioStudies iTest agent submission");
        assertAttribute(rootSection.getAttributes().get(1), "alias", "alias-901047");

        assertThat(rootSection.getLinks(), empty());
        assertThat(rootSection.getSubsections(), empty());
    }

    private void assertAttribute(BioStudiesAttribute attribute, String expectedName, String expectedValue) {
        assertEquals(attribute.getName(), expectedName);
        assertEquals(attribute.getValue(), expectedValue);
    }

    private BioStudiesSubmission waitForSubmissionProcessing()
    throws InterruptedException {
        int waits = 0;
        BioStudiesSubmission submission = null;

        while (waits < 5) {
            try {
                submission = session.getSubmission("S-SUBSTEST123");
            } catch (Exception e) {
                submission = null;
            }

            if (submission == null) {
                waits++;
                Thread.sleep(10000);
            } else {
                break;
            }
        }

        return submission;
    }

    private void publishTestMessage()
    throws IOException, TimeoutException {
        String message = TestUtil.readFile("TestSubmissionMessage.json");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQProperties.getHost());
        connectionFactory.setUsername(rabbitMQProperties.getUsername());
        connectionFactory.setPassword(rabbitMQProperties.getPassword());
        connectionFactory.setVirtualHost(rabbitMQProperties.getVirtualHost());

        Channel channel = connectionFactory.newConnection().createChannel();
        channel.basicPublish("", Queues.BIOSTUDIES_AGENT, null, message.getBytes());
    }
}
