
export interface LoadingPlanCargo {
    id: string
    loadingPlanId: string
    cargoId: string
    quantity: number
    createdAt: string
    updatedAt: string
}

export interface AddLoadingPlanCargoRequest {
    cargoId: string
    quantity: number
}

export interface UpdateLoadingPlanCargoRequest {
    quantity: number
}

async function handleResponse<T>(
    response: Response
): Promise<T> {
    if (!response.ok) {
        let message = `Request failed with HTTP ${response.status}`

        try {
            const body = (await response.json()) as {
                error?: string
                message?: string
            }

            message = body.error ?? body.message ?? message
        } catch {
            // Keep the HTTP status if no JSON error exists.
        }

        throw new Error(message)
    }

    return (await response.json()) as T
}

export async function addLoadingPlanCargo(
    planId: string,
    request: AddLoadingPlanCargoRequest
): Promise<LoadingPlanCargo> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/cargo`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    return handleResponse<LoadingPlanCargo>(response)
}

export async function getLoadingPlanCargo(
    planId: string
): Promise<LoadingPlanCargo[]> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/cargo`
    )

    return handleResponse<LoadingPlanCargo[]>(response)
}

export async function updateLoadingPlanCargoQuantity(
    planId: string,
    cargoLineId: string,
    quantity: number
): Promise<LoadingPlanCargo> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/cargo/${encodeURIComponent(cargoLineId)}`,
        {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ quantity })
        }
    )

    return handleResponse<LoadingPlanCargo>(response)
}

export async function removeLoadingPlanCargo(
    planId: string,
    cargoLineId: string
): Promise<void> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/cargo/${encodeURIComponent(cargoLineId)}`,
        {
            method: 'DELETE'
        }
    )

    if (!response.ok) {
        let message = `Request failed with HTTP ${response.status}`

        try {
            const body = (await response.json()) as {
                error?: string
                message?: string
            }

            message = body.error ?? body.message ?? message
        } catch {
            // Keep the HTTP status if no JSON error exists.
        }

        throw new Error(message)
    }
}
