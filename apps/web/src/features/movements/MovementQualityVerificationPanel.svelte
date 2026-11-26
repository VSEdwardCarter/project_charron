<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getManifests,
        getManifestLifecycle,
        getManifestFreshness,
        type MovementManifest,
        type ManifestFreshnessAssessment
    } from '../../lib/api/movementManifests'

    import {
        getQualityVerifications,
        getQualityVerificationStatus,
        completeQualityVerification,
        type MovementQualityVerification,
        type QualityVerificationChecks,
        type VerificationStatus
    } from '../../lib/api/movementQualityVerifications'

    let {
        movementId
    }: {
        movementId: string
    } = $props()

    // Temporary development reviewer identity.
    const DEVELOPMENT_REVIEWER_ID =
        '00000000-0000-4000-8000-000000000002'

    const checklist: {
        key: keyof QualityVerificationChecks
        label: string
        description: string
    }[] = [
        {
            key: 'equipmentAssignmentVerified',
            label: 'Equipment Assignment',
            description:
                'Confirm physical equipment matches the issued manifest.'
        },
        {
            key: 'equipmentInspectionCompleted',
            label: 'Equipment Inspection',
            description:
                'Confirm required physical equipment inspections have been completed.'
        },
        {
            key: 'cargoReconciliationVerified',
            label: 'Cargo Reconciliation',
            description:
                'Confirm physical cargo identities and quantities match the issued manifest.'
        },
        {
            key: 'loadingArrangementVerified',
            label: 'Loading Arrangement',
            description:
                'Confirm the physical cargo arrangement matches the approved loading plan.'
        },
        {
            key: 'cargoSecurementVerified',
            label: 'Cargo Securement',
            description:
                'Confirm required cargo securement checks have been completed.'
        },
        {
            key: 'documentationVerified',
            label: 'Documentation',
            description:
                'Confirm required movement documentation is present and accurate.'
        }
    ]

    function emptyChecks(): QualityVerificationChecks {
        return {
            equipmentAssignmentVerified: false,
            equipmentInspectionCompleted: false,
            cargoReconciliationVerified: false,
            loadingArrangementVerified: false,
            cargoSecurementVerified: false,
            documentationVerified: false
        }
    }

    let issuedManifest = $state<MovementManifest | null>(null)

    let freshness =
        $state<ManifestFreshnessAssessment | null>(null)

    let history =
        $state<MovementQualityVerification[]>([])

    let verificationStatus =
        $state<VerificationStatus | null>(null)

    let checks = $state<QualityVerificationChecks>(
        emptyChecks()
    )

    let remarks = $state('')
    let loading = $state(true)
    let submitting = $state(false)
    let error = $state('')
    let success = $state('')

    let completedCount = $derived(
        checklist.filter(item => checks[item.key]).length
    )

    let eligible = $derived(
        issuedManifest !== null &&
        freshness?.status === 'CURRENT' &&
        verificationStatus !== null &&
        verificationStatus !== 'NOT_ELIGIBLE'
    )

    let canSubmit = $derived(
        eligible &&
        completedCount === checklist.length &&
        !loading &&
        !submitting
    )

    onMount(() => {
        void refresh()
    })

    async function refresh() {

        loading = true
        error = ''
        issuedManifest = null
        freshness = null
        history = []
        verificationStatus = null
        success = ''
        checks = emptyChecks()
        remarks = ''

        try {

            const manifests = await getManifests(movementId)

            const manifestStates = await Promise.all(
                manifests.map(async manifest => ({
                    manifest,
                    lifecycle: await getManifestLifecycle(
                        movementId,
                        manifest.id
                    )
                }))
            )

            const issued = manifestStates.filter(
                item => item.lifecycle.status === 'ISSUED'
            )

            if (issued.length > 1) {
                throw new Error(
                    'Multiple issued manifests detected.'
                )
            }

            if (issued.length === 0) {
                return
            }

            issuedManifest = issued[0].manifest

            const manifestId = issuedManifest.id

            const [
                freshnessResult,
                historyResult,
                statusResult
            ] = await Promise.all([
                getManifestFreshness(
                    movementId,
                    manifestId
                ),
                getQualityVerifications(
                    movementId,
                    manifestId
                ),
                getQualityVerificationStatus(
                    movementId,
                    manifestId
                )
            ])

            freshness = freshnessResult
            history = historyResult
            verificationStatus = statusResult.status

        } catch (err) {

            error = err instanceof Error
                ? err.message
                : 'Unable to load quality verification.'

        } finally {

            loading = false
        }
    }

    async function submitVerification() {

        if (!canSubmit || !issuedManifest) return

        submitting = true
        error = ''
        success = ''

        try {

            await completeQualityVerification(
                movementId,
                issuedManifest.id,
                {
                    reviewerId: DEVELOPMENT_REVIEWER_ID,
                    ...checks,
                    remarks: remarks.trim() || null
                }
            )

            await refresh()

            success = 'Quality verification recorded successfully.'

        } catch (err) {

            const message = err instanceof Error
                ? err.message
                : 'Unable to complete quality verification.'

            await refresh()

            error = message

        } finally {

            submitting = false
        }
    }

    function verificationStatusLabel(
        status: VerificationStatus | null
    ): string {

        switch (status) {
            case 'VERIFIED':
                return 'Verified'

            case 'VERIFICATION_REQUIRED':
                return 'Verification Required'

            case 'REVERIFICATION_REQUIRED':
                return 'Reverification Required'

            case 'NOT_ELIGIBLE':
                return 'Not Eligible'

            default:
                return 'Unknown'
        }
    }

    function verificationStatusColor(
        status: VerificationStatus | null
    ): string {

        switch (status) {
            case 'VERIFIED':
                return 'text-emerald-400'

            case 'VERIFICATION_REQUIRED':
            case 'REVERIFICATION_REQUIRED':
                return 'text-amber-400'

            case 'NOT_ELIGIBLE':
                return 'text-red-400'

            default:
                return 'text-slate-400'
        }
    }

    function formatDate(value: string): string {
        return new Date(value).toLocaleString()
    }
</script>

<section class="space-y-5">

    <!-- QA/QC Header -->

    <div class="rounded-xl border border-slate-800
                bg-slate-900 p-5">

        <div class="flex flex-wrap items-center
                    justify-between gap-3">

            <div>
                <h3 class="text-lg font-semibold text-white">
                    Operational QA/QC
                </h3>

                <p class="mt-1 text-sm text-slate-400">
                    Physical verification against the issued manifest.
                </p>
            </div>

            <button
                    type="button"
                    onclick={refresh}
                    disabled={loading || submitting}
                    class="rounded-lg border border-slate-700
                       px-4 py-2 text-sm text-slate-300
                       hover:bg-slate-800 disabled:opacity-50"
            >
                Refresh
            </button>

        </div>

        {#if error}
            <div class="mt-4 rounded-lg border border-red-800
                        bg-red-950/40 p-3 text-sm text-red-300">
                {error}
            </div>
        {/if}

        {#if success}
            <div class="mt-4 rounded-lg border border-emerald-800
                        bg-emerald-950/40 p-3 text-sm text-emerald-300">
                {success}
            </div>
        {/if}

        {#if loading}

            <p class="mt-5 text-sm text-slate-400">
                Loading quality verification workspace...
            </p>

        {:else if !issuedManifest && !error}

            <div class="mt-5 rounded-lg border border-amber-800
                        bg-amber-950/40 p-4 text-sm text-amber-300">

                An issued manifest is required before
                operational quality verification can begin.

            </div>

        {:else if issuedManifest && freshness}

            <div class="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">

                <div class="rounded-lg border border-slate-800 p-4">
                    <p class="text-xs text-slate-400">
                        Issued Manifest
                    </p>

                    <p class="mt-2 font-semibold text-white">
                        Version {issuedManifest.version}
                    </p>
                </div>

                <div class="rounded-lg border border-slate-800 p-4">
                    <p class="text-xs text-slate-400">
                        Manifest Freshness
                    </p>

                    <p class="mt-2 font-semibold
                              {eligible
                                ? 'text-emerald-400'
                                : 'text-red-400'}">
                        {freshness.status}
                    </p>
                </div>

                <div class="rounded-lg border border-slate-800 p-4">
                    <p class="text-xs text-slate-400">
                        Recorded Verifications
                    </p>

                    <p class="mt-2 font-semibold text-white">
                        {history.length}
                    </p>
                </div>
                <div class="rounded-lg border border-slate-800 p-4">

                    <p class="text-xs text-slate-400">
                        Verification Status
                    </p>

                    <p class="mt-2 font-semibold
                            {verificationStatusColor(verificationStatus)}">

                        {verificationStatusLabel(verificationStatus)}

                    </p>

                </div>

            </div>

            {#if verificationStatus === 'REVERIFICATION_REQUIRED'}

                <div class="mt-4 rounded-lg border border-amber-800
                bg-amber-950/40 p-4 text-sm text-amber-300">

                    Operational changes have invalidated previous
                    quality verifications. A new physical verification
                    must be completed before dispatch readiness
                    requirements can be satisfied.

                    Previous verification records remain available
                    in the historical record.

                </div>

            {/if}

            {#if !eligible}
                <div class="mt-4 rounded-lg border border-amber-800
                            bg-amber-950/40 p-4 text-sm text-amber-300">

                    Quality verification is unavailable because
                    manifest freshness cannot be confirmed.

                    {#if freshness.reason}
                        <p class="mt-2">{freshness.reason}</p>
                    {/if}

                </div>
            {/if}

        {/if}

    </div>

    <!-- Verification Checklist -->

    {#if eligible && !loading}

        <div class="rounded-xl border border-slate-800
                    bg-slate-900 p-5">

            <div class="flex items-center justify-between">

                <h3 class="font-semibold text-white">
                    Operator Verification Checklist
                </h3>

                <span class="text-sm text-slate-400">
                    {completedCount} / {checklist.length}
                </span>

            </div>

            <div class="mt-4 h-2 overflow-hidden
                        rounded-full bg-slate-800">

                <div
                        class="h-full rounded-full bg-emerald-600
                           transition-all"
                        style:width={`${completedCount /
                        checklist.length * 100}%`}
                ></div>

            </div>

            <div class="mt-5 space-y-3">

                {#each checklist as item (item.key)}

                    <label class="flex cursor-pointer items-start
                                  gap-4 rounded-lg border
                                  border-slate-800 p-4
                                  hover:bg-slate-800/50">

                        <input
                                type="checkbox"
                                bind:checked={checks[item.key]}
                                disabled={submitting}
                                class="mt-1 h-5 w-5 accent-emerald-600"
                        />

                        <div>

                            <p class="text-sm font-medium text-white">
                                {item.label}
                            </p>

                            <p class="mt-1 text-sm text-slate-400">
                                {item.description}
                            </p>

                        </div>

                    </label>

                {/each}

            </div>

            <label
                    for="quality-remarks"
                    class="mt-5 block text-sm font-medium text-slate-300"
            >
                Reviewer Remarks
            </label>

            <textarea
                    id="quality-remarks"
                    bind:value={remarks}
                    disabled={submitting}
                    rows="3"
                    placeholder="Record inspection observations..."
                    class="mt-2 w-full rounded-lg border
                       border-slate-700 bg-slate-950
                       p-3 text-sm text-slate-200"
            ></textarea>

            <button
                    type="button"
                    onclick={submitVerification}
                    disabled={!canSubmit}
                    class="mt-4 rounded-lg bg-emerald-600
                       px-5 py-2.5 text-sm font-medium
                       text-white hover:bg-emerald-500
                       disabled:cursor-not-allowed
                       disabled:opacity-40"
            >
                {submitting
                    ? 'Recording Verification...'
                    : 'Complete Quality Verification'}
            </button>

            <p class="mt-3 text-xs text-amber-400">
                Development mode — authenticated reviewer
                identity is not yet implemented.
            </p>

            <p class="mt-2 text-xs text-slate-400">
                Verification does not constitute dispatch authorization.
            </p>

        </div>

    {/if}

    <!-- Verification History -->

    {#if issuedManifest && !loading}

        <div class="rounded-xl border border-slate-800
                    bg-slate-900 p-5">

            <h3 class="font-semibold text-white">
                Verification History
            </h3>

            <p class="mt-1 text-sm text-slate-400">
                Completed records for manifest Version
                {issuedManifest.version}.
            </p>

            {#if history.length === 0}

                <p class="mt-4 text-sm text-slate-500">
                    No completed verifications recorded.
                </p>

            {:else}

                <div class="mt-4 space-y-3">

                    {#each history as record (record.id)}

                        <article class="rounded-lg border
                                        border-slate-800 p-4">

                            <div class="flex flex-wrap items-center
                                        justify-between gap-2">

                                <span class="text-sm font-medium
                                             text-emerald-400">
                                    Verification Completed
                                </span>

                                <span class="text-xs text-slate-400">
                                    {formatDate(record.verifiedAt)}
                                </span>

                            </div>

                            <p class="mt-2 break-all text-xs
                                      text-slate-400">
                                Reviewer: {record.reviewerId}
                            </p>

                            <p class="mt-2 text-sm text-slate-300">
                                All six required checks acknowledged.
                            </p>

                            {#if record.remarks}
                                <p class="mt-2 text-sm text-slate-300">
                                    {record.remarks}
                                </p>
                            {/if}

                        </article>

                    {/each}

                </div>

            {/if}

        </div>

    {/if}

</section>