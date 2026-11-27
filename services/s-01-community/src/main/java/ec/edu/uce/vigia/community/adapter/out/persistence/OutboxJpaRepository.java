package ec.edu.uce.vigia.community.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
