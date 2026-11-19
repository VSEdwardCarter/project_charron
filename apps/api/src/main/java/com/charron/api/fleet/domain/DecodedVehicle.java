
package com.charron.api.fleet.domain;

public record DecodedVehicle(
        String vin,
        String errorCode,
        String errorText,

        String manufacturer,
        String make,
        String model,
        Integer modelYear,
        String trim,
        String vehicleType,
        String bodyClass,

        String engineManufacturer,
        String engineModel,
        String engineConfiguration,
        Integer engineCylinders,
        Double displacementLiters,
        Double engineHorsepower,
        String fuelType,

        String transmission,
        String driveType,
        String gvwrClass
) {
}
