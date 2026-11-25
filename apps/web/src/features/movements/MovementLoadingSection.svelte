
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getMovementEquipment,
        type MovementEquipmentAssignment
    } from '../../lib/api/movementEquipment'

    import {
        getEquipmentCargoSpaces,
        type EquipmentCargoSpace
    } from '../../lib/api/equipmentCargoSpaces'

    import LoadingWorkspace
        from '../loading/LoadingWorkspace.svelte'

    interface Equipment {
        id: string
        name: string
        category: string
        vin: string | null
    }

    interface LoadingOption {
        equipment: Equipment
        space: EquipmentCargoSpace
    }

    interface Props {
        movementId: string
        movementReference: string
    }

    let { movementId, movementReference }: Props = $props()

    let loading = $state(true)
    let error = $state<string | null>(null)

    let options: LoadingOption[] = $state([])
    let selectedSpaceId = $state('')
    let viewingWorkspace = $state(false)

    let selectedOption = $derived(
        options.find(
            option => option.space.id === selectedSpaceId
        ) ?? null
    )

    async function loadOptions(): Promise<void> {
        loading = true
        error = null

        try {
            const [assignments, response] = await Promise.all([
                getMovementEquipment(movementId),
                fetch('/api/equipment')
            ])

            if (!response.ok) {
                throw new Error(
                    'Unable to retrieve fleet equipment'
                )
            }

            const equipment = await response.json() as Equipment[]

            const equipmentById = new Map(
                equipment.map(item => [item.id, item])
            )

            const assignedIds = [
                ...new Set(
                    assignments.map(
                        (assignment: MovementEquipmentAssignment) =>
                            assignment.equipmentId
                    )
                )
            ]

            const results = await Promise.all(
                assignedIds.map(async equipmentId => {
                    const item = equipmentById.get(equipmentId)

                    if (!item) {
                        throw new Error(
                            `Assigned equipment not found: ${equipmentId}`
                        )
                    }

                    const spaces =
                        await getEquipmentCargoSpaces(equipmentId)

                    return spaces.map(space => ({
                        equipment: item,
                        space
                    }))
                })
            )

            options = results.flat()

            if (!options.some(
                option => option.space.id === selectedSpaceId
            )) {
                selectedSpaceId = ''
                viewingWorkspace = false
            }

        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to load movement cargo spaces'

            options = []
            selectedSpaceId = ''
            viewingWorkspace = false
        } finally {
            loading = false
        }
    }

    function openWorkspace(): void {
        if (!selectedOption) return

        if (selectedOption.space.usableHeightMm === null) {
            return
        }

        viewingWorkspace = true
    }

    onMount(() => {
        void loadOptions()
    })
</script>

<section class="space-y-5">

    {#if viewingWorkspace && selectedOption}

        {@const option = selectedOption}

        {#if option.space.usableHeightMm !== null}
            {#key option.space.id}
                <LoadingWorkspace
                        equipmentName={option.equipment.name}
                        cargoSpaceId={option.space.id}
                        cargoSpaceName={option.space.name}
                        lengthMm={option.space.usableLengthMm}
                        widthMm={option.space.usableWidthMm}
                        heightMm={option.space.usableHeightMm}
                        equipmentId={option.equipment.id}
                        {movementId}
                        {movementReference}
                        onBack={() => viewingWorkspace = false}
                />
            {/key}
        {/if}

    {:else}

        <div>
            <h3 class="text-lg font-semibold text-white">
                Movement Loading
            </h3>

            <p class="mt-1 text-sm text-slate-400">
                Select assigned equipment and a cargo space
                to manage loading arrangements.
            </p>
        </div>

        {#if loading}

            <p class="text-sm text-slate-400">
                Loading assigned cargo spaces...
            </p>

        {:else if error}

            <div role="alert"
                 class="rounded-lg border border-red-800 p-4">

                <p class="text-sm text-red-400">
                    {error}
                </p>

                <button
                        type="button"
                        onclick={loadOptions}
                        class="mt-3 text-sm text-emerald-400"
                >
                    Retry
                </button>
            </div>

        {:else if options.length === 0}

            <div class="rounded-xl border border-slate-800
                        bg-slate-900 p-5">

                <p class="text-sm text-slate-300">
                    No cargo spaces are available on equipment
                    assigned to this movement.
                </p>

                <p class="mt-2 text-sm text-slate-400">
                    Assign equipment in the Equipment tab.
                    Cargo spaces can be configured in Fleet.
                </p>
            </div>

        {:else}

            <div class="space-y-4 rounded-xl
                        border border-slate-800
                        bg-slate-900 p-5">

                <div>
                    <label
                            for="movement-loading-space"
                            class="mb-2 block text-sm text-slate-300"
                    >
                        Equipment / Cargo Space
                    </label>

                    <select
                            id="movement-loading-space"
                            bind:value={selectedSpaceId}
                            class="w-full rounded-lg border
                               border-slate-700 bg-slate-950
                               px-3 py-2 text-white"
                    >
                        <option value="">
                            Select cargo space...
                        </option>

                        {#each options as option (option.space.id)}
                            <option value={option.space.id}>
                                {option.equipment.name}
                                — {option.space.name}
                            </option>
                        {/each}
                    </select>
                </div>

                {#if selectedOption}

                    <div class="rounded-lg border border-slate-700
                                bg-slate-950 p-4 text-sm">

                        <p class="font-medium text-white">
                            {selectedOption.equipment.name}
                        </p>

                        <p class="mt-1 text-slate-400">
                            {selectedOption.space.name}
                            ({selectedOption.space.type.replaceAll('_', ' ')})
                        </p>

                        <p class="mt-2 text-slate-400">
                            Usable dimensions:
                            {selectedOption.space.usableLengthMm}
                            × {selectedOption.space.usableWidthMm}
                            × {selectedOption.space.usableHeightMm ?? 'Unknown'}
                            mm
                        </p>

                        {#if selectedOption.space.usableHeightMm === null}
                            <p class="mt-3 text-amber-400">
                                A usable height must be configured
                                before opening the 3D planner.
                            </p>
                        {/if}
                    </div>

                {/if}

                <div class="flex justify-between gap-3">

                    <button
                            type="button"
                            onclick={loadOptions}
                            class="rounded-lg border border-slate-700
                               px-4 py-2 text-sm text-slate-300
                               hover:bg-slate-800"
                    >
                        Refresh
                    </button>

                    <button
                            type="button"
                            onclick={openWorkspace}
                            disabled={
                            !selectedOption ||
                            selectedOption.space.usableHeightMm === null
                        }
                            class="rounded-lg bg-emerald-600
                               px-5 py-2 text-sm font-medium
                               text-white hover:bg-emerald-500
                               disabled:cursor-not-allowed
                               disabled:opacity-50"
                    >
                        Open Loading Workspace
                    </button>

                </div>
            </div>

        {/if}

    {/if}

</section>
