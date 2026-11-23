
export interface LoadingPlan {
    id: string
    movementId: string
    equipmentId: string
    cargoSpaceId: string
    createdAt: string
    updatedAt: string
}

export interface CreateLoadingPlanRequest {
    equipmentId: string
    cargoSpaceId: string
}

async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        let message = `Request failed with HTTP ${response.status}`

        try {
            const body = (await response.json()) as {
                error?: string
                message?: string
            }

            message = body.error ?? body.message ?? message
        } catch {
            // Preserve the HTTP status if no JSON error body exists.
        }

        throw new Error(message)
    }

    return (await response.json()) as T
}

export async function createLoadingPlan(
    movementId: string,
    request: CreateLoadingPlanRequest
): Promise<LoadingPlan> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(movementId)}/loading-plans`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    return handleResponse<LoadingPlan>(response)
}

export async function getMovementLoadingPlans(
    movementId: string
): Promise<LoadingPlan[]> {
    const response = await fetch(
        `/api/movements/${encodeURIComponent(movementId)}/loading-plans`
    )

    return handleResponse<LoadingPlan[]>(response)
}

export async function getLoadingPlan(
    planId: string
): Promise<LoadingPlan> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}`
    )

    return handleResponse<LoadingPlan>(response)
}
