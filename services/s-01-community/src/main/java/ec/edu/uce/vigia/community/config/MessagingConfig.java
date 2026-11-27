package ec.edu.uce.vigia.community.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** RabbitMQ queue read by n8n (ADR-019, ADR-039) and the scheduler of the outbox relay. */
@Configuration
@EnableScheduling
public class MessagingConfig {

    @Bean
    Queue automationQueue() {
        return new Queue("automation.n8n", true);
    }
}
