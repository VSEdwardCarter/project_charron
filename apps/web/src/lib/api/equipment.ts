
import type { Equipment } from '../models/equipment'

export async function getEquipment(): Promise<Equipment[]> {
    const response = await fetch('/api/equipment')

    if (!response.ok) {
        throw new Error(
            `Failed to retrieve equipment (${response.status})`
        )
    }

    return (await response.json()) as Equipment[]
}

export interface EquipmentRegistrationData {
    name: string
    category: Equipment['category']
    vin: string | null
}

export async function registerEquipment(
    data: EquipmentRegistrationData
): Promise<Equipment> {
    const response = await fetch('/api/equipment', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    })

    if (!response.ok) {
        throw new Error(
            `Failed to register equipment (${response.status})`
        )
    }

    return (await response.json()) as Equipment
}
