
export interface Cargo {
    id: string
    name: string
    reference: string | null
    description: string | null
    weightGrams: number | null
    lengthMm: number | null
    widthMm: number | null
    heightMm: number | null
}

export async function getCargo(): Promise<Cargo[]> {
    const response = await fetch('/api/cargo')

    if (!response.ok) {
        throw new Error(
            `Unable to retrieve cargo: HTTP ${response.status}`
        )
    }

    return (await response.json()) as Cargo[]
}


export interface CargoRegistrationData {
    name: string
    reference: string | null
    description: string | null
    weightGrams: number | null
    lengthMm: number | null
    widthMm: number | null
    heightMm: number | null
}

export async function registerCargo(
    data: CargoRegistrationData
): Promise<Cargo> {
    const response = await fetch('/api/cargo', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })

    if (!response.ok) {
        throw new Error(
            `Unable to register cargo: HTTP ${response.status}`
        )
    }

    return (await response.json()) as Cargo
}
