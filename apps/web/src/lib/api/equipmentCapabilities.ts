
export const equipmentCapabilityLabels = {
    PROPULSION: 'Propulsion',
    TOWING: 'Towing',
    CARGO_TRANSPORT: 'Cargo Transport',
    ENCLOSED_CARGO: 'Enclosed Cargo',
    TEMPERATURE_CONTROL: 'Temperature Control',
    MATERIAL_HANDLING: 'Material Handling',
    LIFTING: 'Lifting',
} as const

export type EquipmentCapability =
    keyof typeof equipmentCapabilityLabels

export async function getEquipmentCapabilities(
    equipmentId: string
): Promise<EquipmentCapability[]> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/capabilities`
    )

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve capabilities (${response.status})`
        )
    }

    return (await response.json()) as EquipmentCapability[]
}

export async function addEquipmentCapability(
    equipmentId: string,
    capability: EquipmentCapability
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/capabilities`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ capability }),
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to assign capability (${response.status})`
        )
    }
}

export async function removeEquipmentCapability(
    equipmentId: string,
    capability: EquipmentCapability
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/capabilities/${capability}`,
        {
            method: 'DELETE',
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to remove capability (${response.status})`
        )
    }
}
