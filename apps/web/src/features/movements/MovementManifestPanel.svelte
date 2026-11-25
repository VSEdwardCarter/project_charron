<script lang="ts">
    import ManifestLifecyclePanel from './ManifestLifecyclePanel.svelte'
    import { onMount } from 'svelte'

    import {
        generateManifest,
        getManifests,
        type MovementManifest
    } from '../../lib/api/movementManifests'

    interface Props {
        movementId: string
        movementReference: string
    }

    let { movementId, movementReference }: Props = $props()

    let manifests = $state<MovementManifest[]>([])
    let selectedManifest = $state<MovementManifest | null>(null)

    let loading = $state(true)
    let generating = $state(false)
    let error = $state('')
    let success = $state('')

    let totalCargoUnits = $derived(
        selectedManifest?.cargo.reduce(
            (total, item) => total + item.quantity,
            0
        ) ?? 0
    )

    let totalCargoWeight = $derived(
        selectedManifest &&
        selectedManifest.cargo.every(
            item => item.unitWeightGrams !== null
        )
            ? selectedManifest.cargo.reduce(
                (total, item) =>
                    total +
                    (item.unitWeightGrams ?? 0) *
                    item.quantity,
                0
            )
            : null
    )

    function sortManifests(
        items: MovementManifest[]
    ): MovementManifest[] {
        return [...items].sort(
            (a, b) => b.version - a.version
        )
    }

    async function loadHistory(): Promise<void> {
        loading = true
        error = ''

        try {
            const items = await getManifests(movementId)

            manifests = sortManifests(items)

            selectedManifest =
                manifests.find(
                    item => item.id === selectedManifest?.id
                ) ??
                manifests[0] ??
                null

        } catch (exception) {
            error = exception instanceof Error
                ? exception.message
                : 'Unable to retrieve manifest history'
        } finally {
            loading = false
        }
    }

    async function handleGenerate(): Promise<void> {
        if (generating || loading) {
            return
        }

        generating = true
        error = ''
        success = ''

        try {
            const manifest = await generateManifest(movementId)

            manifests = sortManifests([
                manifest,
                ...manifests.filter(
                    item => item.id !== manifest.id
                )
            ])

            selectedManifest = manifest

            success =
                `Manifest version ${manifest.version} generated successfully.`

        } catch (exception) {
            error = exception instanceof Error
                ? exception.message
                : 'Unable to generate manifest'
        } finally {
            generating = false
        }
    }

    function formatDate(value: string): string {
        return new Date(value).toLocaleString()
    }

    function formatWeight(value: number | null): string {
        if (value === null) {
            return 'Unknown'
        }

        return `${(value / 1000).toLocaleString(
            undefined,
            { maximumFractionDigits: 3 }
        )} kg`
    }

    onMount(() => {
        void loadHistory()
    })
</script>

<section class="space-y-5">

    <!-- Manifest Header -->

    <div class="rounded-xl border border-slate-800
                bg-slate-900 p-5">

        <div class="flex flex-wrap items-start
                    justify-between gap-4">

            <div>
                <h3 class="text-lg font-semibold text-white">
                    Movement Manifests
                </h3>

                <p class="mt-1 text-sm text-slate-400">
                    Generate and retrieve historical manifests
                    for {movementReference}.
                </p>
            </div>

            <div class="flex flex-wrap gap-2">

                <button
                        type="button"
                        onclick={loadHistory}
                        disabled={loading || generating}
                        class="rounded-lg border border-slate-700
                           px-4 py-2 text-sm text-slate-300
                           hover:bg-slate-800
                           disabled:opacity-50"
                >
                    Refresh
                </button>

                <button
                        type="button"
                        onclick={handleGenerate}
                        disabled={generating || loading}
                        class="rounded-lg bg-emerald-600
                           px-4 py-2 text-sm font-medium
                           text-white hover:bg-emerald-500
                           disabled:opacity-50"
                >
                    {generating
                        ? 'Generating...'
                        : 'Generate Manifest'}
                </button>

            </div>

        </div>

        {#if error}
            <div
                    role="alert"
                    class="mt-4 rounded-lg border border-red-900
                       bg-red-950/40 p-3 text-sm text-red-300"
            >
                {error}
            </div>
        {/if}

        {#if success}
            <div
                    role="status"
                    class="mt-4 rounded-lg border border-emerald-900
                       bg-emerald-950/40 p-3
                       text-sm text-emerald-300"
            >
                {success}
            </div>
        {/if}

    </div>

    {#if loading}

        <p class="p-5 text-sm text-slate-400">
            Loading manifest history...
        </p>

    {:else if manifests.length === 0}

        <div class="rounded-xl border border-dashed
                    border-slate-700 p-10 text-center">

            <h4 class="font-medium text-white">
                {error
                    ? 'Manifest history unavailable'
                    : 'No manifests generated'}
            </h4>

            <p class="mt-2 text-sm text-slate-400">
                {error
                    ? 'Refresh to retry retrieving the manifest history.'
                    : 'Generate a manifest to capture the current movement details.'}
            </p>

        </div>

    {:else}

        <div class="grid gap-5 lg:grid-cols-[260px_minmax(0,1fr)]">

            <!-- Version History -->

            <aside class="rounded-xl border border-slate-800
                          bg-slate-900 p-4">

                <h4 class="mb-4 text-sm font-semibold text-white">
                    Version History
                </h4>

                <div class="space-y-2">

                    {#each manifests as manifest (manifest.id)}

                        <button
                                type="button"
                                onclick={() => selectedManifest = manifest}
                                aria-pressed={selectedManifest?.id === manifest.id}
                                class="w-full rounded-lg border p-3
                                   text-left transition-colors
                                   {selectedManifest?.id === manifest.id
                                       ? 'border-emerald-600 bg-emerald-950/40'
                                       : 'border-slate-800 hover:bg-slate-800'}"
                        >

                            <div class="flex items-center
                                        justify-between gap-2">

                                <span class="text-sm font-medium
                                             text-white">
                                    Version {manifest.version}
                                </span>

                                {#if manifest.id === manifests[0]?.id}
                                    <span class="rounded bg-slate-800
                                                 px-2 py-1 text-xs
                                                 text-slate-300">
                                        Latest
                                    </span>
                                {/if}

                            </div>

                            <p class="mt-2 text-xs text-slate-400">
                                {formatDate(manifest.generatedAt)}
                            </p>

                        </button>

                    {/each}

                </div>

            </aside>

            {#if selectedManifest}


                <article class="min-w-0 space-y-5">

{#key selectedManifest.id}
                    <ManifestLifecyclePanel
                        movementId={selectedManifest.movementId}
                        manifestId={selectedManifest.id}
                    />
                {/key}

                    <!-- Manifest Summary -->

                    <div class="rounded-xl border border-slate-800
                                bg-slate-900 p-5">

                        <p class="text-xs font-medium uppercase
                                  tracking-wider text-slate-400">
                            Movement Manifest
                        </p>

                        <h3 class="mt-2 text-xl font-semibold
                                   text-white">
                            {selectedManifest.movementReference}
                        </h3>

                        <p class="mt-1 text-sm text-slate-400">
                            Version {selectedManifest.version}
                            · Generated
                            {formatDate(selectedManifest.generatedAt)}
                        </p>

                        <dl class="mt-5 grid gap-5 sm:grid-cols-2">

                            <div>
                                <dt class="text-xs text-slate-400">
                                    Origin
                                </dt>
                                <dd class="mt-1 text-sm text-white">
                                    {selectedManifest.origin}
                                </dd>
                            </div>

                            <div>
                                <dt class="text-xs text-slate-400">
                                    Destination
                                </dt>
                                <dd class="mt-1 text-sm text-white">
                                    {selectedManifest.destination}
                                </dd>
                            </div>

                            <div>
                                <dt class="text-xs text-slate-400">
                                    Planned Departure
                                </dt>
                                <dd class="mt-1 text-sm text-white">
                                    {formatDate(
                                        selectedManifest.plannedDeparture
                                    )}
                                </dd>
                            </div>

                            <div>
                                <dt class="text-xs text-slate-400">
                                    Planned Arrival
                                </dt>
                                <dd class="mt-1 text-sm text-white">
                                    {formatDate(
                                        selectedManifest.plannedArrival
                                    )}
                                </dd>
                            </div>

                        </dl>

                    </div>

                    <!-- Summary Metrics -->

                    <div class="grid gap-3 sm:grid-cols-2
                                xl:grid-cols-4">

                        <div class="rounded-xl border border-slate-800
                                    bg-slate-900 p-4">

                            <p class="text-xs text-slate-400">
                                Equipment Assignments
                            </p>

                            <p class="mt-2 text-2xl font-semibold
                                      text-white">
                                {selectedManifest.equipment.length}
                            </p>

                        </div>

                        <div class="rounded-xl border border-slate-800
                                    bg-slate-900 p-4">

                            <p class="text-xs text-slate-400">
                                Loading Plans
                            </p>

                            <p class="mt-2 text-2xl font-semibold
                                      text-white">
                                {selectedManifest.loadingPlans.length}
                            </p>

                        </div>

                        <div class="rounded-xl border border-slate-800
                                    bg-slate-900 p-4">

                            <p class="text-xs text-slate-400">
                                Cargo Units
                            </p>

                            <p class="mt-2 text-2xl font-semibold
                                      text-white">
                                {totalCargoUnits.toLocaleString()}
                            </p>

                        </div>

                        <div class="rounded-xl border border-slate-800
                                    bg-slate-900 p-4">

                            <p class="text-xs text-slate-400">
                                Total Cargo Weight
                            </p>

                            <p class="mt-2 text-xl font-semibold
                                      text-white">
                                {formatWeight(totalCargoWeight)}
                            </p>

                        </div>

                    </div>

                    <!-- Equipment -->

                    <section class="rounded-xl border border-slate-800
                                    bg-slate-900 p-5">

                        <h4 class="font-semibold text-white">
                            Assigned Equipment
                        </h4>

                        {#if selectedManifest.equipment.length === 0}

                            <p class="mt-4 text-sm text-slate-400">
                                No equipment recorded.
                            </p>

                        {:else}

                            <div class="mt-4 overflow-x-auto">

                                <table class="w-full text-left text-sm">

                                    <thead class="text-xs uppercase
                                                  text-slate-400">

                                    <tr>
                                        <th class="pb-3 pr-4">
                                            Equipment
                                        </th>
                                        <th class="pb-3 pr-4">
                                            Role
                                        </th>
                                        <th class="pb-3">
                                            VIN
                                        </th>
                                    </tr>

                                    </thead>

                                    <tbody class="divide-y
                                                  divide-slate-800">

                                    {#each selectedManifest.equipment as item}

                                        <tr>
                                            <td class="py-3 pr-4
                                                           text-white">
                                                {item.name}
                                            </td>

                                            <td class="py-3 pr-4
                                                           text-slate-300">
                                                {item.role.replaceAll('_', ' ')}
                                            </td>

                                            <td class="py-3
                                                           text-slate-400">
                                                {item.vin ?? '—'}
                                            </td>
                                        </tr>

                                    {/each}

                                    </tbody>

                                </table>

                            </div>

                        {/if}

                    </section>

                    <!-- Cargo -->

                    <section class="rounded-xl border border-slate-800
                                    bg-slate-900 p-5">

                        <h4 class="font-semibold text-white">
                            Cargo Manifest
                        </h4>

                        {#if selectedManifest.cargo.length === 0}

                            <p class="mt-4 text-sm text-slate-400">
                                No cargo recorded.
                            </p>

                        {:else}

                            <div class="mt-4 overflow-x-auto">

                                <table class="w-full text-left text-sm">

                                    <thead class="text-xs uppercase
                                                  text-slate-400">

                                    <tr>
                                        <th class="pb-3 pr-4">
                                            Cargo
                                        </th>
                                        <th class="pb-3 pr-4">
                                            Quantity
                                        </th>
                                        <th class="pb-3 pr-4">
                                            Dimensions (mm)
                                        </th>
                                        <th class="pb-3">
                                            Unit Weight
                                        </th>
                                    </tr>

                                    </thead>

                                    <tbody class="divide-y
                                                  divide-slate-800">

                                    {#each selectedManifest.cargo as item}

                                        <tr>
                                            <td class="py-3 pr-4
                                                           text-white">
                                                {item.name}
                                            </td>

                                            <td class="py-3 pr-4
                                                           text-slate-300">
                                                {item.quantity}
                                            </td>

                                            <td class="py-3 pr-4
                                                           text-slate-300
                                                           whitespace-nowrap">

                                                {#if item.lengthMm !== null &&
                                                item.widthMm !== null &&
                                                item.heightMm !== null}

                                                    {item.lengthMm}
                                                    × {item.widthMm}
                                                    × {item.heightMm}

                                                {:else}
                                                    Unknown
                                                {/if}

                                            </td>

                                            <td class="py-3
                                                           text-slate-300">
                                                {formatWeight(
                                                    item.unitWeightGrams
                                                )}
                                            </td>
                                        </tr>

                                    {/each}

                                    </tbody>

                                </table>

                            </div>

                        {/if}

                    </section>

                    <!-- Loading Plans -->

                    <section class="rounded-xl border border-slate-800
                                    bg-slate-900 p-5">

                        <h4 class="font-semibold text-white">
                            Loading Plans
                        </h4>

                        {#if selectedManifest.loadingPlans.length === 0}

                            <p class="mt-4 text-sm text-slate-400">
                                No loading plans recorded.
                            </p>

                        {:else}

                            <div class="mt-4 space-y-3">

                                {#each selectedManifest.loadingPlans as plan}

                                    <div class="rounded-lg border
                                                border-slate-800 p-4">

                                        <div class="flex flex-wrap
                                                    items-center
                                                    justify-between gap-3">

                                            <p class="text-sm
                                                      font-medium text-white">

                                                {selectedManifest.equipment.find(
                                                    item =>
                                                        item.equipmentId ===
                                                        plan.equipmentId
                                                )?.name ?? plan.equipmentId}

                                            </p>

                                            <span class="rounded-md border
                                                         border-slate-700
                                                         px-2 py-1 text-xs
                                                         text-slate-300">

                                                {plan.arrangementStatus.replaceAll(
                                                    '_', ' '
                                                )}

                                            </span>

                                        </div>

                                        <p class="mt-2 break-all
                                                  text-xs text-slate-400">
                                            Loading plan:
                                            {plan.loadingPlanId}
                                        </p>

                                        <p class="mt-1 break-all
                                                  text-xs text-slate-400">
                                            Cargo space:
                                            {plan.cargoSpaceId}
                                        </p>

                                    </div>

                                {/each}

                            </div>

                        {/if}

                    </section>

                    <p class="text-xs text-slate-500">
                        This manifest is a preserved historical
                        snapshot. Generation does not constitute
                        review, issuance, or dispatch authorization.
                    </p>

                </article>

            {/if}

        </div>

    {/if}

</section>