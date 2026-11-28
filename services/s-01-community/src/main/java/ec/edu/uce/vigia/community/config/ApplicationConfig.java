package ec.edu.uce.vigia.community.config;

import ec.edu.uce.vigia.community.application.CommunityService;
import ec.edu.uce.vigia.community.application.port.EventOutbox;
import ec.edu.uce.vigia.community.application.port.PersonRepository;
import ec.edu.uce.vigia.community.application.port.VehicleRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the use cases to their ports; the application layer has no Spring stereotypes (ADR-037). */
@Configuration
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CommunityService communityService(
            PersonRepository people, VehicleRepository vehicles, EventOutbox outbox, Clock clock) {
        return new CommunityService(people, vehicles, outbox, clock);
    }
}
