
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.DecodedVehicle;

public interface VehicleVinDecoder {

    DecodedVehicle decode(String vin);

    VinDecodeSnapshot decodeSnapshot(String vin);
}
