package uk.ac.ebi.subs.biostudies.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
    private BioStudiesSubmission submission;

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
    throws Exception
     {
        publishTestMessage();
        waitForSubmission();

        // TODO verify all properties
        // TODO simplify test submission message
        assertNotNull("The submission wasn't found", submission);
        assertEquals(submission.getAccno(), "S-SUBSTEST123");
     }

    private void waitForSubmission()
    throws InterruptedException{
        int waits = 0;

        // TODO change to while to wait last
        do {
            waits++;
            Thread.sleep(10000);
            submission = session.getSubmission("S-SUBSTEST123");
        } while (submission == null || waits < 5);
    }

    private void publishTestMessage() {
        String message = TestUtil.readFile("TestSubmissionMessage.json");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQProperties.getHost());
        connectionFactory.setUsername(rabbitMQProperties.getUsername());
        connectionFactory.setPassword(rabbitMQProperties.getPassword());
        connectionFactory.setVirtualHost(rabbitMQProperties.getVirtualHost());

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.basicPublish("", Queues.BIOSTUDIES_AGENT, null, message.getBytes());
        } catch (IOException | TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }
}

