
import type { CargoPlacement3D } from '../models/cargoPlacement'

export interface LoadingPreviewCargoItem {
    cargoId: string
    name: string
    quantity: number
    lengthMm: number
    widthMm: number
    heightMm: number
    weightGrams: number | null
}

export interface LoadingPreviewRequest {
    cargoSpaceId: string
    cargo: LoadingPreviewCargoItem[]
}

export interface LoadingPreviewPlacement extends CargoPlacement3D {
    loadCargoLineId: string
}

export type LoadingPreviewStatus =
    | 'VALID'
    | 'INVALID'
    | 'INDETERMINATE'
    | 'NOT_PLACED'

export interface LoadingPreviewResponse {
    cargoSpaceId: string
    status: LoadingPreviewStatus
    reason: string
    placements: LoadingPreviewPlacement[]
}

export async function generateLoadingPreview(
    request: LoadingPreviewRequest
): Promise<LoadingPreviewResponse> {
    const response = await fetch('/api/loading/preview', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
    })

    if (!response.ok) {
        throw new Error(
            `Loading preview request failed: HTTP ${response.status}`
        )
    }

    return (await response.json()) as LoadingPreviewResponse
}
