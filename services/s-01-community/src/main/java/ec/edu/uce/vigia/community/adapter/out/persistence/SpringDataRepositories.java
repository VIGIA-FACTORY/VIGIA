package ec.edu.uce.vigia.community.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PersonJpaRepository extends JpaRepository<PersonEntity, UUID> {

    boolean existsByDocumentNumber(String documentNumber);

    List<PersonEntity> findAllByOrderByLastNameAscFirstNameAsc();
}

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {

    boolean existsByPlate(String plate);

    List<VehicleEntity> findAllByOrderByPlateAsc();
}
