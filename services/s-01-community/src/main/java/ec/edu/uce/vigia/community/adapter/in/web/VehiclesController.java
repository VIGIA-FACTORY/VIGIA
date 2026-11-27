package ec.edu.uce.vigia.community.adapter.in.web;

import ec.edu.uce.vigia.community.application.CommunityService;
import ec.edu.uce.vigia.community.domain.Vehicle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /v1/vehicles}: register and list vehicles of community members (US-041). */
@RestController
@RequestMapping("/v1/vehicles")
public class VehiclesController {

    private final CommunityService service;

    public VehiclesController(CommunityService service) {
        this.service = service;
    }

    public record RegisterVehicleRequest(
            @NotBlank String plate, @NotNull UUID ownerId, String brand, String model, String color) {}

    /** Response with the plate as text, as the clients show it. */
    public record VehicleResponse(UUID id, String plate, UUID ownerId, String brand, String model, String color) {

        static VehicleResponse of(Vehicle vehicle) {
            return new VehicleResponse(
                    vehicle.id(),
                    vehicle.plate().value(),
                    vehicle.ownerId(),
                    vehicle.brand(),
                    vehicle.model(),
                    vehicle.color());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse register(@Valid @RequestBody RegisterVehicleRequest request) {
        return VehicleResponse.of(service.registerVehicle(
                request.plate(), request.ownerId(), request.brand(), request.model(), request.color()));
    }

    @GetMapping
    public List<VehicleResponse> list() {
        return service.vehicles().stream().map(VehicleResponse::of).toList();
    }
}
