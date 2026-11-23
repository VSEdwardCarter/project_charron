
import type {
    LoadingPreviewRequest,
    LoadingPreviewResponse,
    LoadingPreviewStatus
} from './loadingPreview'

export interface LoadingPlanArrangement {
    loadingPlanId: string
    formatVersion: number
    requestSnapshot: string
    previewSnapshot: string
    status: LoadingPreviewStatus
    generatedAt: string
    savedAt: string
    updatedAt: string
}

export class ArrangementApiError extends Error {
    constructor(
        message: string,
        public readonly status: number
    ) {
        super(message)
        this.name = 'ArrangementApiError'
    }
}

async function handleResponse<T>(
    response: Response
): Promise<T> {
    if (!response.ok) {
        let message = `Request failed: HTTP ${response.status}`

        try {
            const body = (await response.json()) as {
                error?: string
                message?: string
            }

            message = body.error ?? body.message ?? message
        } catch {
            // Preserve the HTTP status when no JSON body exists.
        }

        throw new ArrangementApiError(message, response.status)
    }

    return (await response.json()) as T
}

export async function generateAndSaveArrangement(
    planId: string
): Promise<LoadingPlanArrangement> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/arrangement/generate`,
        {
            method: 'POST'
        }
    )

    return handleResponse<LoadingPlanArrangement>(response)
}

export async function getSavedArrangement(
    planId: string
): Promise<LoadingPlanArrangement> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/arrangement`
    )

    return handleResponse<LoadingPlanArrangement>(response)
}

export async function getCurrentArrangement(
    planId: string
): Promise<LoadingPlanArrangement> {
    const response = await fetch(
        `/api/loading-plans/${encodeURIComponent(planId)}/arrangement/current`
    )

    return handleResponse<LoadingPlanArrangement>(response)
}

export function parseArrangementPreview(
    arrangement: LoadingPlanArrangement
): LoadingPreviewResponse {
    if (arrangement.formatVersion !== 1) {
        throw new Error(
            `Unsupported arrangement format: ${arrangement.formatVersion}`
        )
    }

    const preview: unknown = JSON.parse(
        arrangement.previewSnapshot
    )

    if (
        typeof preview !== 'object' ||
        preview === null ||
        !('cargoSpaceId' in preview) ||
        typeof preview.cargoSpaceId !== 'string' ||
        !('status' in preview) ||
        !['VALID', 'INVALID', 'INDETERMINATE', 'NOT_PLACED']
            .includes(String(preview.status)) ||
        !('reason' in preview) ||
        typeof preview.reason !== 'string' ||
        !('placements' in preview) ||
        !Array.isArray(preview.placements)
    ) {
        throw new Error('Saved arrangement has an invalid preview format')
    }

    return preview as LoadingPreviewResponse
}

export function parseArrangementRequest(
    arrangement: LoadingPlanArrangement
): LoadingPreviewRequest {
    const request: unknown = JSON.parse(
        arrangement.requestSnapshot
    )

    if (
        typeof request !== 'object' ||
        request === null ||
        !('cargoSpaceId' in request) ||
        typeof request.cargoSpaceId !== 'string' ||
        !('cargo' in request) ||
        !Array.isArray(request.cargo)
    ) {
        throw new Error('Saved arrangement has an invalid request format')
    }

    return request as LoadingPreviewRequest
}
