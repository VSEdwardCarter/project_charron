
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.DecodedVehicle;

import java.util.List;
import java.util.Map;

public record VinDecodeSnapshot(
        DecodedVehicle decodedVehicle,
        List<Map<String, Object>> rawResponse
) {
}
