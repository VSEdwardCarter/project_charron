
export type MovementEquipmentRole =
    | 'POWER_UNIT'
    | 'CARGO_CARRIER'
    | 'SUPPORT'

export interface MovementEquipmentAssignment {
    id: string
    movementId: string
    equipmentId: string
    role: MovementEquipmentRole
    assignedAt: string
}

export interface MovementEquipmentAssignmentRequest {
    equipmentId: string
    role: MovementEquipmentRole
}

export async function getMovementEquipment(
    movementId: string
): Promise<MovementEquipmentAssignment[]> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(movementId)}/equipment`
    )

    if (!response.ok) {
        const problem = await response.json().catch(() => null)

        throw new Error(
            problem?.detail ??
            `Unable to retrieve movement equipment: HTTP ${response.status}`
        )
    }

    return response.json()
}

export async function assignMovementEquipment(
    movementId: string,
    request: MovementEquipmentAssignmentRequest
): Promise<MovementEquipmentAssignment> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(movementId)}/equipment`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    if (!response.ok) {
        const problem = await response.json().catch(() => null)

        throw new Error(
            problem?.detail ??
            `Unable to assign equipment: HTTP ${response.status}`
        )
    }

    return response.json()
}
