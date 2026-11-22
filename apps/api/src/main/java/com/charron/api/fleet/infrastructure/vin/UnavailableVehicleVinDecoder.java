
package com.charron.api.fleet.infrastructure.vin;

import com.charron.api.fleet.application.VehicleVinDecoder;
import com.charron.api.fleet.application.VinDecodeSnapshot;
import com.charron.api.fleet.domain.DecodedVehicle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(
        name = "charron.vpic.enabled",
        havingValue = "false"
)
public class UnavailableVehicleVinDecoder
        implements VehicleVinDecoder {

    @Override
    public DecodedVehicle decode(String vin) {
        throw unavailable();
    }

    @Override
    public VinDecodeSnapshot decodeSnapshot(String vin) {
        throw unavailable();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "VIN decoding is not configured in this environment"
        );
    }
}
