
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getEquipment
    } from '../../lib/api/equipment'

    import type {
        Equipment
    } from '../../lib/models/equipment'

    import {
        getEquipmentCargoSpaces,
        type EquipmentCargoSpace
    } from '../../lib/api/equipmentCargoSpaces'

    import LoadingWorkspace from '../loading/LoadingWorkspace.svelte'

    let equipment: Equipment[] = $state([])
    let spaces: EquipmentCargoSpace[] = $state([])

    let selectedEquipmentId = $state('')
    let selectedCargoSpaceId = $state('')
    let workspaceOpen = $state(false)

    let loadingEquipment = $state(true)
    let loadingSpaces = $state(false)
    let error: string | null = $state(null)

    let spaceRequestId = 0

    const selectedEquipment = $derived(
        equipment.find(item => item.id === selectedEquipmentId)
    )

    const selectedCargoSpace = $derived(
        spaces.find(item => item.id === selectedCargoSpaceId)
    )

    async function loadEquipment(): Promise<void> {
        loadingEquipment = true
        error = null

        try {
            equipment = await getEquipment()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve equipment'
        } finally {
            loadingEquipment = false
        }
    }

    async function selectEquipment(id: string): Promise<void> {
        const requestId = ++spaceRequestId

        selectedEquipmentId = id
        selectedCargoSpaceId = ''
        spaces = []
        workspaceOpen = false
        error = null

        if (!id) {
            loadingSpaces = false
            return
        }

        loadingSpaces = true

        try {
            const result = await getEquipmentCargoSpaces(id)

            if (requestId !== spaceRequestId) return

            spaces = result
        } catch (err) {
            if (requestId !== spaceRequestId) return

            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve cargo spaces'
        } finally {
            if (requestId === spaceRequestId) {
                loadingSpaces = false
            }
        }
    }

    function openWorkspace(): void {
        if (!selectedEquipment || !selectedCargoSpace) return

        if (selectedCargoSpace.usableHeightMm === null) {
            error = 'A defined cargo-space height is required for the 3D preview.'
            return
        }

        error = null
        workspaceOpen = true
    }

    onMount(() => {
        void loadEquipment()
    })
</script>

{#if workspaceOpen &&
selectedEquipment &&
selectedCargoSpace &&
selectedCargoSpace.usableHeightMm !== null}

    <LoadingWorkspace
            equipmentName={selectedEquipment.name}
            cargoSpaceId={selectedCargoSpace.id}
            cargoSpaceName={selectedCargoSpace.name}
            lengthMm={selectedCargoSpace.usableLengthMm}
            widthMm={selectedCargoSpace.usableWidthMm}
            heightMm={selectedCargoSpace.usableHeightMm}
            onBack={() => workspaceOpen = false}
    />

{:else}

    <div class="space-y-6">
        <div>
            <h3 class="text-xl font-semibold text-white">
                Movement Planning
            </h3>

            <p class="mt-2 text-sm text-slate-400">
                Select equipment and a cargo space to begin planning a load.
            </p>
        </div>

        {#if error}
            <div
                    role="alert"
                    class="rounded-lg border border-red-800
               bg-red-950/40 p-3 text-sm text-red-300"
            >
                {error}
            </div>
        {/if}

        <div class="grid gap-5 md:grid-cols-2">
            <div class="space-y-2">
                <label
                        for="movement-equipment"
                        class="block text-sm font-medium text-slate-300"
                >
                    Equipment
                </label>

                <select
                        id="movement-equipment"
                        value={selectedEquipmentId}
                        onchange={event =>
            void selectEquipment(event.currentTarget.value)
          }
                        disabled={loadingEquipment}
                        class="w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2.5 text-sm text-white"
                >
                    <option value="">
                        {loadingEquipment
                            ? 'Loading equipment...'
                            : 'Select equipment...'}
                    </option>

                    {#each equipment as item (item.id)}
                        <option value={item.id}>
                            {item.name}
                        </option>
                    {/each}
                </select>
            </div>

            <div class="space-y-2">
                <label
                        for="movement-cargo-space"
                        class="block text-sm font-medium text-slate-300"
                >
                    Cargo Space
                </label>

                <select
                        id="movement-cargo-space"
                        bind:value={selectedCargoSpaceId}
                        disabled={!selectedEquipmentId || loadingSpaces}
                        class="w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2.5 text-sm text-white"
                >
                    <option value="">
                        {loadingSpaces
                            ? 'Loading cargo spaces...'
                            : 'Select cargo space...'}
                    </option>

                    {#each spaces as space (space.id)}
                        <option
                                value={space.id}
                                disabled={space.usableHeightMm === null}
                        >
                            {space.name}
                            {space.usableHeightMm === null
                                ? ' (height not defined)'
                                : ''}
                        </option>
                    {/each}
                </select>
            </div>
        </div>

        {#if selectedEquipmentId && !loadingSpaces && spaces.length === 0}
            <p class="text-sm text-amber-400">
                This equipment has no registered cargo spaces.
                Add one from Fleet before planning a load.
            </p>
        {/if}

        {#if selectedCargoSpace}
            <div class="rounded-xl border border-slate-800
                  bg-slate-950 p-4 text-sm">
                <h4 class="font-medium text-white">
                    Selected Cargo Space
                </h4>

                <p class="mt-2 text-slate-400">
                    {selectedCargoSpace.usableLengthMm} ×
                    {selectedCargoSpace.usableWidthMm} ×
                    {selectedCargoSpace.usableHeightMm ?? 'Unknown'} mm
                </p>
            </div>
        {/if}

        <div class="flex justify-end">
            <button
                    type="button"
                    onclick={openWorkspace}
                    disabled={
          !selectedEquipment ||
          !selectedCargoSpace ||
          selectedCargoSpace.usableHeightMm === null
        }
                    class="rounded-lg bg-emerald-600 px-5 py-2.5
               text-sm font-medium text-white
               hover:bg-emerald-500
               disabled:cursor-not-allowed
               disabled:opacity-50"
            >
                Open Loading Workspace
            </button>
        </div>
    </div>

{/if}
