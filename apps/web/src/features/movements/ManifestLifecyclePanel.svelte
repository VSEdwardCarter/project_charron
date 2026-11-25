<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getManifestLifecycle,
        getManifestFreshness,
        reviewManifest,
        issueManifest,
        type ManifestLifecycle,
        type ManifestFreshnessAssessment
    } from '../../lib/api/movementManifests'

    let {
        movementId,
        manifestId
    }: {
        movementId: string
        manifestId: string
    } = $props()

    // Temporary development identity.
    // Replace with authenticated user identity before production.
    const DEVELOPMENT_ACTOR_ID =
        '00000000-0000-4000-8000-000000000001'

    let lifecycle = $state<ManifestLifecycle | null>(null)
    let freshness = $state<ManifestFreshnessAssessment | null>(null)

    let loading = $state(true)
    let submitting = $state(false)
    let error = $state('')
    let success = $state('')
    let remarks = $state('')

    let canReview = $derived(
        !loading &&
        !submitting &&
        lifecycle?.status === 'DRAFT' &&
        freshness?.status === 'CURRENT'
    )

    let canIssue = $derived(
        !loading &&
        !submitting &&
        lifecycle?.status === 'REVIEWED' &&
        freshness?.status === 'CURRENT'
    )

    onMount(() => {
        void refresh()
    })

    async function refresh() {
        loading = true
        error = ''
        lifecycle = null
        freshness = null

        try {
            const [lifecycleResult, freshnessResult] =
                await Promise.all([
                    getManifestLifecycle(movementId, manifestId),
                    getManifestFreshness(movementId, manifestId)
                ])

            lifecycle = lifecycleResult
            freshness = freshnessResult
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve manifest lifecycle'
        } finally {
            loading = false
        }
    }

    async function transition(action: 'review' | 'issue') {
        if (action === 'review' && !canReview) return
        if (action === 'issue' && !canIssue) return

        submitting = true
        error = ''
        success = ''

        try {
            const request = {
                actorId: DEVELOPMENT_ACTOR_ID,
                remarks: remarks.trim() || null
            }

            lifecycle = action === 'review'
                ? await reviewManifest(
                    movementId,
                    manifestId,
                    request
                )
                : await issueManifest(
                    movementId,
                    manifestId,
                    request
                )

            remarks = ''

            await refresh()

            success = action === 'review'
                ? 'Manifest reviewed successfully.'
                : 'Manifest issued successfully.'

        } catch (err) {
            const message = err instanceof Error
                ? err.message
                : 'Manifest transition failed'

            await refresh()
            error = message

        } finally {
            submitting = false
        }
    }

    function statusClass(status: string): string {
        switch (status) {
            case 'CURRENT':
            case 'ISSUED':
                return 'bg-emerald-950 text-emerald-400'

            case 'REVIEWED':
                return 'bg-blue-950 text-blue-300'

            case 'STALE':
                return 'bg-red-950 text-red-300'

            case 'UNVERIFIABLE':
                return 'bg-amber-950 text-amber-300'

            case 'SUPERSEDED':
                return 'bg-purple-950 text-purple-300'

            default:
                return 'bg-slate-800 text-slate-200'
        }
    }

    function formatDate(value: string): string {
        return new Date(value).toLocaleString()
    }
</script>

<section class="rounded-lg border border-slate-800 bg-slate-900 p-5">

    <div class="mb-4 flex items-center justify-between">
        <div>
            <h3 class="text-lg font-semibold text-white">
                Manifest Lifecycle
            </h3>

            <p class="text-sm text-slate-400">
                Review, issuance, and historical accountability
            </p>
        </div>

        <button
                class="rounded border border-slate-700 px-3 py-2 text-sm
                   hover:bg-slate-800 disabled:opacity-50"
                onclick={refresh}
                disabled={loading || submitting}
        >
            Refresh
        </button>
    </div>

    {#if error}
        <div class="mb-4 rounded border border-red-800 bg-red-950/40 p-3
                    text-sm text-red-300">
            {error}
        </div>
    {/if}

    {#if success}
        <div class="mb-4 rounded border border-emerald-800 bg-emerald-950/40 p-3
                    text-sm text-emerald-400">
            {success}
        </div>
    {/if}

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading manifest lifecycle...
        </p>

    {:else if lifecycle && freshness}

        <div class="mb-5 grid grid-cols-1 gap-4 md:grid-cols-2">

            <div class="rounded border border-slate-800 p-4">
                <p class="mb-2 text-sm text-slate-400">
                    Lifecycle Status
                </p>

                <span class="rounded px-3 py-1 text-sm font-semibold
                             {statusClass(lifecycle.status)}">
                    {lifecycle.status}
                </span>
            </div>

            <div class="rounded border border-slate-800 p-4">
                <p class="mb-2 text-sm text-slate-400">
                    Operational Freshness
                </p>

                <span class="rounded px-3 py-1 text-sm font-semibold
                             {statusClass(freshness.status)}">
                    {freshness.status}
                </span>
            </div>

        </div>

        {#if freshness.status !== 'CURRENT'}

            <div class="mb-4 rounded border border-amber-800
                        bg-amber-950/40 p-4 text-sm text-amber-300">

                <p class="font-semibold">
                    Manifest is not confirmed current
                </p>

                {#if freshness.reason}
                    <p class="mt-1">{freshness.reason}</p>
                {/if}

                {#if freshness.changes.length > 0}
                    <ul class="mt-2 list-disc pl-5">
                        {#each freshness.changes as change}
                            <li>{change.replaceAll('_', ' ')}</li>
                        {/each}
                    </ul>
                {/if}

                <p class="mt-2">
                    Review and issuance are unavailable.
                    Generate a new manifest if operational data has changed.
                </p>

            </div>

        {/if}

        {#if lifecycle.status === 'DRAFT' ||
        lifecycle.status === 'REVIEWED'}

            <div class="mb-5 space-y-3">

                <label
                        for="manifest-remarks"
                        class="block text-sm font-medium text-slate-200"
                >
                    Remarks (optional)
                </label>

                <textarea
                        id="manifest-remarks"
                        bind:value={remarks}
                        disabled={submitting}
                        rows="2"
                        placeholder="Enter review or issuance remarks..."
                        class="w-full rounded border border-slate-700 p-2 text-sm"
                ></textarea>

                <div class="flex gap-3">

                    <button
                            onclick={() => transition('review')}
                            disabled={!canReview}
                            class="rounded bg-blue-600 px-4 py-2 text-sm
                               font-medium text-white hover:bg-blue-700
                               disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        Review Manifest
                    </button>

                    <button
                            onclick={() => transition('issue')}
                            disabled={!canIssue}
                            class="rounded bg-green-600 px-4 py-2 text-sm
                               font-medium text-white hover:bg-green-700
                               disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        Issue Manifest
                    </button>

                </div>

                <p class="text-xs text-amber-400">
                    Development mode — authenticated operator identity
                    is not yet implemented.
                </p>

            </div>

        {/if}

        <div class="border-t border-slate-800 pt-4">

            <h4 class="mb-3 font-semibold text-white">
                Lifecycle History
            </h4>

            <div class="mb-3 rounded bg-slate-800 p-3 text-sm">
                <p class="font-medium text-slate-200">
                    Manifest Generated
                </p>

                <p class="text-slate-400">
                    {formatDate(lifecycle.generatedAt)}
                </p>
            </div>

            {#each lifecycle.events as event (event.id)}

                <div class="mb-3 rounded border border-slate-800 p-3">

                    <div class="flex flex-wrap items-center gap-2 text-sm">

                        <span class="rounded px-2 py-1
                                     {statusClass(event.previousStatus)}">
                            {event.previousStatus}
                        </span>

                        <span class="text-slate-400">→</span>

                        <span class="rounded px-2 py-1
                                     {statusClass(event.newStatus)}">
                            {event.newStatus}
                        </span>

                    </div>

                    <p class="mt-2 text-xs text-slate-400">
                        {formatDate(event.recordedAt)}
                    </p>

                    <p class="mt-1 break-all text-xs text-slate-400">
                        Actor: {event.actorId}
                    </p>

                    {#if event.remarks}
                        <p class="mt-2 text-sm text-slate-200">
                            {event.remarks}
                        </p>
                    {/if}

                </div>

            {/each}

        </div>

    {/if}

</section>