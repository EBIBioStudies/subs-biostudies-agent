package uk.ac.ebi.subs.biostudies.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.biostudies.client.BioStudiesClientTestContextConfiguration;
import uk.ac.ebi.subs.messaging.Queues;

public class RabbitMQUtil {
    public static void publishMessage() {
        String envelope = TestUtil.readFile("TestSubmissionMessage.json");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("mac-subs-009.ebi.ac.uk");
        connectionFactory.setUsername("subscored");
        connectionFactory.setPassword("nikto");
        connectionFactory.setVirtualHost("subs_dev");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.basicPublish("", Queues.BIOSTUDIES_AGENT, null, envelope.getBytes());
        } catch (IOException | TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }
}
