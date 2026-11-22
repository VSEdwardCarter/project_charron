
export type MovementStatus =
    | 'PLANNED'
    | 'DISPATCHED'
    | 'IN_TRANSIT'
    | 'COMPLETED'
    | 'CANCELLED'

export interface Movement {
    id: string
    reference: string
    origin: string
    destination: string
    plannedDeparture: string
    plannedArrival: string
    status: MovementStatus
    createdAt: string
    updatedAt: string
}

export interface MovementCreationRequest {
    reference: string
    origin: string
    destination: string
    plannedDeparture: string
    plannedArrival: string
}

export async function getMovements(): Promise<Movement[]> {
    const response = await fetch('/api/movements')

    if (!response.ok) {
        throw new Error(
            `Unable to retrieve movements: HTTP ${response.status}`
        )
    }

    return response.json()
}

export async function getMovement(id: string): Promise<Movement> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(id)}`
    )

    if (!response.ok) {
        throw new Error(
            `Unable to retrieve movement: HTTP ${response.status}`
        )
    }

    return response.json()
}

export async function createMovement(
    request: MovementCreationRequest
): Promise<Movement> {
    const response = await fetch('/api/movements', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
    })

    if (!response.ok) {
        const problem = await response.json().catch(() => null)

        throw new Error(
            problem?.detail ??
            `Unable to create movement: HTTP ${response.status}`
        )
    }

    return response.json()
}
