export interface QualityVerificationChecks {
    equipmentAssignmentVerified: boolean
    equipmentInspectionCompleted: boolean
    cargoReconciliationVerified: boolean
    loadingArrangementVerified: boolean
    cargoSecurementVerified: boolean
    documentationVerified: boolean
}

export interface MovementQualityVerification
    extends QualityVerificationChecks {

    id: string
    manifestId: string
    reviewerId: string
    verifiedAt: string
    remarks: string | null
}

export interface QualityVerificationRequest
    extends QualityVerificationChecks {

    reviewerId: string
    remarks: string | null
}

export type VerificationStatus =
    | 'NOT_ELIGIBLE'
    | 'VERIFICATION_REQUIRED'
    | 'REVERIFICATION_REQUIRED'
    | 'VERIFIED'

export interface VerificationStatusResponse {
    status: VerificationStatus
}

export class QualityVerificationApiError extends Error {

    constructor(
        message: string,
        public readonly status: number
    ) {
        super(message)
        this.name = 'QualityVerificationApiError'
    }
}

function verificationUrl(
    movementId: string,
    manifestId: string
): string {

    return `/api/movements/${encodeURIComponent(movementId)}` +
        `/manifests/${encodeURIComponent(manifestId)}` +
        `/quality-verifications`
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

        throw new QualityVerificationApiError(
            message,
            response.status
        )
    }

    return (await response.json()) as T
}

export async function getQualityVerifications(
    movementId: string,
    manifestId: string
): Promise<MovementQualityVerification[]> {

    const response = await fetch(
        verificationUrl(movementId, manifestId)
    )

    return handleResponse<MovementQualityVerification[]>(
        response
    )
}

export async function completeQualityVerification(
    movementId: string,
    manifestId: string,
    request: QualityVerificationRequest
): Promise<MovementQualityVerification> {

    const response = await fetch(
        verificationUrl(movementId, manifestId),
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        }
    )

    return handleResponse<MovementQualityVerification>(
        response
    )
}

export async function getQualityVerificationStatus(
    movementId: string,
    manifestId: string
): Promise<VerificationStatusResponse> {

    const response = await fetch(
        `${verificationUrl(movementId, manifestId)}/status`,
        {
            cache: 'no-store'
        }
    )

    return handleResponse<VerificationStatusResponse>(
        response
    )
}