
export type EquipmentModificationType =
    | 'INSTALLATION'
    | 'REMOVAL'
    | 'REPLACEMENT'
    | 'ALTERATION'

export interface EquipmentModification {
    id: string
    equipmentId: string
    configurationId: string | null
    type: EquipmentModificationType
    description: string
    effectiveAt: string
    recordedAt: string
    removedAt: string | null
}

export interface RecordEquipmentModification {
    type: EquipmentModificationType
    description: string
    effectiveAt: string
}

export async function getEquipmentModifications(
    equipmentId: string
): Promise<EquipmentModification[]> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/modifications`
    )

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve modifications (${response.status})`
        )
    }

    return (await response.json()) as EquipmentModification[]
}

export async function recordEquipmentModification(
    equipmentId: string,
    data: RecordEquipmentModification
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/modifications`,
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
            `Failed to record modification (${response.status})`
        )
    }
}

export async function removeEquipmentModification(
    equipmentId: string,
    modificationId: string,
    effectiveAt: string
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/modifications/${encodeURIComponent(modificationId)}/remove`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ effectiveAt }),
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to remove modification (${response.status})`
        )
    }
}
