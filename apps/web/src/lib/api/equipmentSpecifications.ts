
export interface EquipmentSpecifications {
    lengthMm: number | null
    widthMm: number | null
    heightMm: number | null
    weightSpecificationsPresent: boolean
    emptyWeightGrams: number | null
    grossWeightRatingGrams: number | null
    ratedPayloadCapacityGrams: number | null
}

export async function getEquipmentSpecifications(
    equipmentId: string
): Promise<EquipmentSpecifications | null> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/specifications`
    )

    if (response.status === 204) {
        return null
    }

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve equipment specifications (${response.status})`
        )
    }

    return (await response.json()) as EquipmentSpecifications
}
