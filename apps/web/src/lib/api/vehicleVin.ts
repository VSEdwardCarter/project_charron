
export interface DecodedVehicle {
    vin: string
    errorCode: string | null
    errorText: string | null
    manufacturer: string | null
    make: string | null
    model: string | null
    modelYear: number | null
    trim: string | null
    vehicleType: string | null
    bodyClass: string | null
    engineManufacturer: string | null
    engineModel: string | null
    engineConfiguration: string | null
    engineCylinders: number | null
    displacementLiters: number | null
    engineHorsepower: number | null
    fuelType: string | null
    transmission: string | null
    driveType: string | null
    gvwrClass: string | null
}

export async function decodeVehicleVin(
    vin: string
): Promise<DecodedVehicle> {
    const response = await fetch(
        `/api/vehicles/decode/${encodeURIComponent(vin)}`
    )

    if (!response.ok) {
        throw new Error(
            `VIN decoding failed (${response.status})`
        )
    }

    return (await response.json()) as DecodedVehicle
}
