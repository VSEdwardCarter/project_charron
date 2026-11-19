
export interface EquipmentConfiguration {
    id: string
    equipmentId: string
    name: string
    active: boolean
}

export async function getEquipmentConfigurations(
    equipmentId: string
): Promise<EquipmentConfiguration[]> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations`
    )

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve configurations (${response.status})`
        )
    }

    return (await response.json()) as EquipmentConfiguration[]
}

export interface EquipmentConfigurationInput {
    name: string
    active: boolean
}

export async function createEquipmentConfiguration(
    equipmentId: string,
    data: EquipmentConfigurationInput
): Promise<EquipmentConfiguration> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations`,
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
            `Failed to create configuration (${response.status})`
        )
    }

    return (await response.json()) as EquipmentConfiguration
}


export async function updateEquipmentConfiguration(
    equipmentId: string,
    configurationId: string,
    data: EquipmentConfigurationInput
): Promise<EquipmentConfiguration> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations/${encodeURIComponent(configurationId)}`,
        {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to update configuration (${response.status})`
        )
    }

    return (await response.json()) as EquipmentConfiguration
}


