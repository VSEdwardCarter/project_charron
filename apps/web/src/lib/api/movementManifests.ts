export type MovementEquipmentRole =
    | 'POWER_UNIT'
    | 'CARGO_CARRIER'
    | 'SUPPORT'

export interface ManifestEquipmentEntry {
    equipmentId: string
    name: string
    role: MovementEquipmentRole
    vin: string | null
}

export interface ManifestCargoEntry {
    loadingPlanId: string
    cargoId: string
    name: string
    quantity: number
    lengthMm: number | null
    widthMm: number | null
    heightMm: number | null
    unitWeightGrams: number | null
}

export interface ManifestLoadingEntry {
    loadingPlanId: string
    equipmentId: string
    cargoSpaceId: string
    arrangementStatus: string
    arrangementSnapshot: string
}

export interface MovementManifest {
    id: string
    movementId: string
    version: number
    movementReference: string
    origin: string
    destination: string
    plannedDeparture: string
    plannedArrival: string
    equipment: ManifestEquipmentEntry[]
    cargo: ManifestCargoEntry[]
    loadingPlans: ManifestLoadingEntry[]
    generatedAt: string
}

export type ManifestStatus =
    | 'DRAFT'
    | 'REVIEWED'
    | 'ISSUED'
    | 'SUPERSEDED'

export type ManifestFreshnessStatus =
    | 'CURRENT'
    | 'STALE'
    | 'UNVERIFIABLE'

export type ManifestChangeType =
    | 'MOVEMENT_REFERENCE'
    | 'ORIGIN'
    | 'DESTINATION'
    | 'PLANNED_DEPARTURE'
    | 'PLANNED_ARRIVAL'
    | 'EQUIPMENT'
    | 'CARGO'
    | 'LOADING_PLANS'

export interface ManifestLifecycleEvent {
    id: string
    manifestId: string
    previousStatus: ManifestStatus
    newStatus: ManifestStatus
    actorId: string
    recordedAt: string
    remarks: string | null
}

export interface ManifestLifecycle {
    manifestId: string
    status: ManifestStatus
    generatedAt: string
    lastTransitionAt: string
    events: ManifestLifecycleEvent[]
}

export interface ManifestFreshnessAssessment {
    manifestId: string
    status: ManifestFreshnessStatus
    changes: ManifestChangeType[]
    assessedAt: string
    reason: string | null
}

export interface ManifestTransitionRequest {
    actorId: string
    remarks: string | null
}

export class ManifestApiError extends Error {
    constructor(
        message: string,
        public readonly status: number
    ) {
        super(message)
        this.name = 'ManifestApiError'
    }
}

async function handleResponse<T>(
    response: Response
): Promise<T> {
    if (!response.ok) {
        let message = `Request failed: HTTP ${response.status}`

        try {
            const body = await response.json()

            message =
                body.detail ??
                body.message ??
                body.error ??
                message
        } catch {
            // Preserve the original HTTP error.
        }

        throw new ManifestApiError(
            message,
            response.status
        )
    }

    return (await response.json()) as T
}

function manifestUrl(movementId: string): string {
    return `/api/movements/${encodeURIComponent(movementId)}/manifests`
}

function manifestVersionUrl(
    movementId: string,
    manifestId: string
): string {
    return `${manifestUrl(movementId)}/${encodeURIComponent(manifestId)}`
}

// Manifest generation and history

export async function generateManifest(
    movementId: string
): Promise<MovementManifest> {
    const response = await fetch(
        manifestUrl(movementId),
        {
            method: 'POST'
        }
    )

    return handleResponse<MovementManifest>(response)
}

export async function getManifests(
    movementId: string
): Promise<MovementManifest[]> {
    const response = await fetch(
        manifestUrl(movementId)
    )

    return handleResponse<MovementManifest[]>(response)
}

export async function getManifest(
    movementId: string,
    manifestId: string
): Promise<MovementManifest> {
    const response = await fetch(
        manifestVersionUrl(movementId, manifestId)
    )

    return handleResponse<MovementManifest>(response)
}

// Manifest lifecycle and freshness

export async function getManifestLifecycle(
    movementId: string,
    manifestId: string
): Promise<ManifestLifecycle> {
    const response = await fetch(
        `${manifestVersionUrl(movementId, manifestId)}/lifecycle`
    )

    return handleResponse<ManifestLifecycle>(response)
}

export async function getManifestFreshness(
    movementId: string,
    manifestId: string
): Promise<ManifestFreshnessAssessment> {
    const response = await fetch(
        `${manifestVersionUrl(movementId, manifestId)}/freshness`
    )

    return handleResponse<ManifestFreshnessAssessment>(response)
}

// Manifest review and issuance

export async function reviewManifest(
    movementId: string,
    manifestId: string,
    request: ManifestTransitionRequest
): Promise<ManifestLifecycle> {
    const response = await fetch(
        `${manifestVersionUrl(movementId, manifestId)}/review`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    return handleResponse<ManifestLifecycle>(response)
}

export async function issueManifest(
    movementId: string,
    manifestId: string,
    request: ManifestTransitionRequest
): Promise<ManifestLifecycle> {
    const response = await fetch(
        `${manifestVersionUrl(movementId, manifestId)}/issue`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    return handleResponse<ManifestLifecycle>(response)
}