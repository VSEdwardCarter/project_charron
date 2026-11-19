
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

export const EQUIPMENT_CAPABILITIES = [
    'PROPULSION',
    'TOWING',
    'CARGO_TRANSPORT',
    'ENCLOSED_CARGO',
    'TEMPERATURE_CONTROL',
    'MATERIAL_HANDLING',
    'LIFTING',
] as const

export type EquipmentCapability =
    (typeof EQUIPMENT_CAPABILITIES)[number]

export async function getConfigurationCapabilities(
    equipmentId: string,
    configurationId: string
): Promise<EquipmentCapability[]> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations/${encodeURIComponent(configurationId)}/capabilities`
    )

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve configuration capabilities (${response.status})`
        )
    }

    return (await response.json()) as EquipmentCapability[]
}

export async function addConfigurationCapability(
    equipmentId: string,
    configurationId: string,
    capability: EquipmentCapability
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations/${encodeURIComponent(configurationId)}/capabilities`,
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
            `Failed to add configuration capability (${response.status})`
        )
    }
}

export async function removeConfigurationCapability(
    equipmentId: string,
    configurationId: string,
    capability: EquipmentCapability
): Promise<void> {
    const response = await fetch(
        `/api/equipment/${encodeURIComponent(equipmentId)}/configurations/${encodeURIComponent(configurationId)}/capabilities/${encodeURIComponent(capability)}`,
        {
            method: 'DELETE',
        }
    )

    if (!response.ok) {
        throw new Error(
            `Failed to remove configuration capability (${response.status})`
        )
    }
}


