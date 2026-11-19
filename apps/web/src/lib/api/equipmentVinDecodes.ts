
import type { DecodedVehicle } from './vehicleVin'

export interface VinDecodeAttribute {
    code: string | null
    variable: string | null
    value: string | null
    groupname: string | null
    [key: string]: unknown
}

export interface VinDecodeSnapshot {
    decodedVehicle: DecodedVehicle
    rawResponse: VinDecodeAttribute[]
}

export async function getLatestVinDecode(
    equipmentId: string
): Promise<VinDecodeSnapshot | null> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/vin-decodes/latest`
    )

    if (response.status === 204) {
        return null
    }

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve VIN information (${response.status})`
        )
    }

    return (await response.json()) as VinDecodeSnapshot
}
