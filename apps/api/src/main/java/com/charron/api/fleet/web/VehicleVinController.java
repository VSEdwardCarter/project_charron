
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.VehicleVinDecoder;
import com.charron.api.fleet.domain.DecodedVehicle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleVinController {

    private final VehicleVinDecoder decoder;

    public VehicleVinController(VehicleVinDecoder decoder) {
        this.decoder = decoder;
    }

    @GetMapping("/decode/{vin}")
    public ResponseEntity<DecodedVehicle> decode(
            @PathVariable String vin
    ) {
        return ResponseEntity.ok(decoder.decode(vin));
    }
}
