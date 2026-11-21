
export type CargoSpaceType =
    | 'ENCLOSED'
    | 'OPEN_DECK'

export interface EquipmentCargoSpace {
    id: string
    equipmentId: string
    name: string
    type: CargoSpaceType
    usableLengthMm: number
    usableWidthMm: number
    usableHeightMm: number | null
}

export interface CreateEquipmentCargoSpaceInput {
    name: string
    type: CargoSpaceType
    usableLengthMm: number
    usableWidthMm: number
    usableHeightMm: number | null
}

export const cargoSpaceTypeLabels: Record<CargoSpaceType, string> = {
    ENCLOSED: 'Enclosed Cargo Space',
    OPEN_DECK: 'Open Deck',
}

export async function getEquipmentCargoSpaces(
    equipmentId: string
): Promise<EquipmentCargoSpace[]> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/cargo-spaces`
    )

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve cargo spaces (${response.status})`
        )
    }

    return (await response.json()) as EquipmentCargoSpace[]
}

export async function createEquipmentCargoSpace(
    equipmentId: string,
    data: CreateEquipmentCargoSpaceInput
): Promise<EquipmentCargoSpace> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/cargo-spaces`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to create cargo space (${response.status})`
        )
    }

    return (await response.json()) as EquipmentCargoSpace
}
