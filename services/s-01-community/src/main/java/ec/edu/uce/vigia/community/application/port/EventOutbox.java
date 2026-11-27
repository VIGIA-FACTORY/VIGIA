package ec.edu.uce.vigia.community.application.port;

import ec.edu.uce.vigia.community.domain.DomainEvent;

/** Stores an event in the same transaction as the change, to be published later (ADR-006). */
public interface EventOutbox {

    void append(DomainEvent event);
}
