package ec.edu.uce.vigia.community.application.port;

import ec.edu.uce.vigia.community.domain.Vehicle;
import java.util.List;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    boolean existsByPlate(String plate);

    List<Vehicle> findAll();
}
