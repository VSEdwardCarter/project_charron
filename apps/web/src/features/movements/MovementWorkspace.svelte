<script lang="ts">
    import MovementQualityVerificationPanel from "./MovementQualityVerificationPanel.svelte"
    import {
        type Movement
    } from '../../lib/api/movements'

    import MovementEquipmentAssignment
        from './MovementEquipmentAssignment.svelte'

    import DispatchAssessmentPanel
        from './DispatchAssessmentPanel.svelte'

    import MovementLoadingSection
        from './MovementLoadingSection.svelte'

    import MovementManifestPanel from "./MovementManifestPanel.svelte"

    type WorkspaceTab =
        | 'overview'
        | 'equipment'
        | 'loading'
        | 'manifests'
        | 'quality'

    interface Props {
        movement: Movement
        onBack: () => void
    }

    let { movement, onBack }: Props = $props()

    let activeTab: WorkspaceTab = $state('overview')

    const tabs: {
        id: WorkspaceTab
        label: string
    }[] = [
        { id: 'overview', label: 'Overview' },
        { id: 'equipment', label: 'Equipment' },
        { id: 'loading', label: 'Loading' },
        { id: 'manifests', label: 'Manifests' },
        { id: 'quality', label: 'QA/QC' }
    ]

    function formatDate(value: string): string {
        return new Date(value).toLocaleString()
    }
</script>

<div class="space-y-6">

    <!-- Workspace Header -->

    <div class="flex flex-wrap items-start
                justify-between gap-4">

        <div class="space-y-2">

            <button
                    type="button"
                    onclick={onBack}
                    class="text-sm text-emerald-400
                       hover:text-emerald-300"
            >
                ← Back to Movements
            </button>

            <h2 class="text-2xl font-semibold text-white">
                {movement.reference}
            </h2>

            <p class="text-sm text-slate-400">
                {movement.origin}
                →
                {movement.destination}
            </p>

        </div>

        <span class="rounded-lg border border-slate-700
                     bg-slate-900 px-3 py-1.5
                     text-xs font-medium text-slate-200">
            {movement.status.replaceAll('_', ' ')}
        </span>

    </div>

    <!-- Workspace Navigation -->

    <nav
            aria-label="Movement workspace"
            class="flex flex-wrap gap-2 border-b
               border-slate-800 pb-3"
    >
        {#each tabs as tab (tab.id)}
            <button
                    type="button"
                    onclick={() => activeTab = tab.id}
                    aria-current={
                        activeTab === tab.id
                            ? 'page'
                            : undefined
                    }
                    class="rounded-lg px-4 py-2
                       text-sm font-medium transition-colors
                       {activeTab === tab.id
                           ? 'bg-emerald-600 text-white'
                           : 'bg-slate-900 text-slate-400 hover:bg-slate-800 hover:text-white'}"
            >
                {tab.label}
            </button>
        {/each}
    </nav>

    <!-- Overview -->

    {#if activeTab === 'overview'}

        <section class="space-y-5">

            <div class="rounded-xl border border-slate-800
                        bg-slate-900 p-5">

                <h3 class="text-lg font-semibold text-white">
                    Movement Overview
                </h3>

                <dl class="mt-4 grid gap-5
                           md:grid-cols-2">

                    <div>
                        <dt class="text-xs text-slate-400">
                            Origin
                        </dt>
                        <dd class="mt-1 text-sm text-white">
                            {movement.origin}
                        </dd>
                    </div>

                    <div>
                        <dt class="text-xs text-slate-400">
                            Destination
                        </dt>
                        <dd class="mt-1 text-sm text-white">
                            {movement.destination}
                        </dd>
                    </div>

                    <div>
                        <dt class="text-xs text-slate-400">
                            Planned Departure
                        </dt>
                        <dd class="mt-1 text-sm text-white">
                            {formatDate(movement.plannedDeparture)}
                        </dd>
                    </div>

                    <div>
                        <dt class="text-xs text-slate-400">
                            Planned Arrival
                        </dt>
                        <dd class="mt-1 text-sm text-white">
                            {formatDate(movement.plannedArrival)}
                        </dd>
                    </div>

                </dl>
            </div>

            <DispatchAssessmentPanel
                    movementId={movement.id}
                    movementReference={movement.reference}
            />

        </section>

    {:else if activeTab === 'equipment'}

        <!-- Equipment Assignments -->

        <section class="rounded-xl border border-slate-800
                        bg-slate-900 p-5">

            {#key movement.id}
                <MovementEquipmentAssignment
                        movementId={movement.id}
                />
            {/key}

        </section>

    {:else if activeTab === 'loading'}

        <!-- Movement Loading -->

        <section class="space-y-5">

            {#key movement.id}
                <MovementLoadingSection
                        movementId={movement.id}
                        movementReference={movement.reference}
                />
            {/key}

        </section>

    {:else if activeTab === "manifests"}

        {#key movement.id}
            <MovementManifestPanel
                movementId={movement.id}
                movementReference={movement.reference}
            />
        {/key}

    {:else if activeTab === 'quality'}

        {#key movement.id}
            <MovementQualityVerificationPanel
                movementId={movement.id}
            />
        {/key}

    {/if}

</div>