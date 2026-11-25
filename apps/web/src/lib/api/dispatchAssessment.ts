
export type DispatchFindingSeverity =
    | 'INFO'
    | 'WARNING'
    | 'BLOCKER'

export interface DispatchFinding {
    code: string
    severity: DispatchFindingSeverity
    description: string
}

export interface DispatchAssessment {
    movementId: string
    findings: DispatchFinding[]
}

export async function getDispatchAssessment(
    movementId: string
): Promise<DispatchAssessment> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(movementId)}/dispatch-assessment`
    )

    if (!response.ok) {
        const problem = await response.json().catch(() => null)

        throw new Error(
            problem?.detail ??
            `Unable to assess movement: HTTP ${response.status}`
        )
    }

    return response.json()
}
