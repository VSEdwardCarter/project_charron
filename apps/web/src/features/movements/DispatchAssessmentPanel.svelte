
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getDispatchAssessment,
        type DispatchAssessment,
        type DispatchFinding
    } from '../../lib/api/dispatchAssessment'

    interface Props {
        movementId: string
        movementReference: string | null
    }

    let {
        movementId,
        movementReference
    }: Props = $props()

    let assessment = $state<DispatchAssessment | null>(null)
    let loading = $state(false)
    let error: string | null = $state(null)

    const blockers = $derived<DispatchFinding[]>(
        assessment?.findings.filter(
            finding => finding.severity === 'BLOCKER'
        ) ?? []
    )

    const warnings = $derived<DispatchFinding[]>(
        assessment?.findings.filter(
            finding => finding.severity === 'WARNING'
        ) ?? []
    )

    const informational = $derived<DispatchFinding[]>(
        assessment?.findings.filter(
            finding => finding.severity === 'INFO'
        ) ?? []
    )

    let requestId = 0

    async function assess(): Promise<void> {
        const currentRequest = ++requestId
        const requestedMovementId = movementId

        loading = true
        error = null
        assessment = null

        try {
            const result = await getDispatchAssessment(
                requestedMovementId
            )

            if (currentRequest !== requestId) return

            assessment = result
        } catch (err) {
            if (currentRequest !== requestId) return

            error = err instanceof Error
                ? err.message
                : 'Unable to assess movement'
        } finally {
            if (currentRequest === requestId) {
                loading = false
            }
        }
    }

    onMount(() => {
        void assess()

        return () => {
            requestId++
        }
    })
</script>

<div class="space-y-4 rounded-xl border border-slate-700
            bg-slate-950 p-5">

    <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
            <h3 class="text-lg font-semibold text-white">
                Pre-Dispatch Assessment
            </h3>

            <p class="mt-1 text-sm text-slate-400">
                {movementReference ?? 'Selected movement'}
            </p>

            <p class="mt-2 text-sm text-slate-400">
                Automated evaluation of movement planning,
                equipment assignments, cargo, and loading arrangements.
            </p>
        </div>

        <button
                type="button"
                onclick={() => void assess()}
                disabled={loading}
                class="rounded-lg border border-slate-600
                       px-4 py-2 text-sm text-white
                       hover:bg-slate-800
                       disabled:opacity-50"
        >
            {loading ? 'Assessing...' : 'Run Assessment'}
        </button>
    </div>

    {#if error}
        <div role="alert"
             class="rounded-lg border border-red-800
                    bg-red-950/40 p-3 text-sm text-red-300">
            {error}
        </div>
    {/if}

    {#if loading}
        <p class="text-sm text-slate-400">
            Evaluating movement readiness...
        </p>
    {:else if assessment}

        {#if blockers.length > 0}
            <div class="rounded-lg border border-red-800
                        bg-red-950/30 p-4">
                <p class="font-medium text-red-300">
                    {blockers.length} blocking
                    {blockers.length === 1 ? ' finding' : ' findings'}
                </p>

                <p class="mt-1 text-sm text-red-200">
                    Correct the following issues before proceeding
                    with pre-dispatch review.
                </p>
            </div>
        {:else}
            <div class="rounded-lg border border-emerald-800
                        bg-emerald-950/30 p-4">
                <p class="font-medium text-emerald-300">
                    No automated blocking findings
                </p>

                <p class="mt-1 text-sm text-slate-300">
                    Documentation, operator QA/QC, and dispatch
                    authorization are still required.
                </p>
            </div>
        {/if}

        {#if assessment.findings.length > 0}
            <div class="space-y-3">
                {#each [
                    ...blockers,
                    ...warnings,
                    ...informational
                ] as finding, index (finding.code + '-' + index)}

                    <div class="rounded-lg border p-3
                                {finding.severity === 'BLOCKER'
                                    ? 'border-red-900 bg-red-950/20'
                                    : finding.severity === 'WARNING'
                                        ? 'border-amber-900 bg-amber-950/20'
                                        : 'border-slate-700 bg-slate-900'}">

                        <div class="flex flex-wrap items-center gap-2">
                            <span class="text-xs font-semibold
                                         {finding.severity === 'BLOCKER'
                                             ? 'text-red-300'
                                             : finding.severity === 'WARNING'
                                                 ? 'text-amber-300'
                                                 : 'text-sky-300'}">
                                {finding.severity}
                            </span>

                            <span class="text-xs text-slate-500">
                                {finding.code}
                            </span>
                        </div>

                        <p class="mt-2 text-sm text-slate-200">
                            {finding.description}
                        </p>
                    </div>
                {/each}
            </div>
        {/if}

        <div class="border-t border-slate-800 pt-3">
            <p class="text-xs text-slate-400">
                Automated assessment only. Physical inspection,
                document reconciliation, QA/QC, and dispatch
                authorization are separate requirements.
            </p>
        </div>
    {/if}
</div>
