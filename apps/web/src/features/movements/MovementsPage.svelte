
<script lang="ts">
    import { onMount } from 'svelte'

    import { getEquipment } from '../../lib/api/equipment'
    import type { Equipment } from '../../lib/models/equipment'

    import {
        getEquipmentCargoSpaces,
        type EquipmentCargoSpace
    } from '../../lib/api/equipmentCargoSpaces'

    import {
        getMovementEquipment,
        type MovementEquipmentAssignment
    } from '../../lib/api/movementEquipment'

    import { getMovement } from '../../lib/api/movements'

    import MovementList from './MovementList.svelte'
    import MovementCreationForm from './MovementCreationForm.svelte'
    import LoadingWorkspace from '../loading/LoadingWorkspace.svelte'

    let equipment: Equipment[] = $state([])
    let spaces: EquipmentCargoSpace[] = $state([])
    let movementAssignments: MovementEquipmentAssignment[] = $state([])

    let movementRefreshKey = $state(0)

    let selectedMovementId = $state<string | null>(null)
    let selectedMovementReference = $state<string | null>(null)

    let selectedEquipmentId = $state('')
    let selectedCargoSpaceId = $state('')
    let workspaceOpen = $state(false)

    let loadingEquipment = $state(true)
    let loadingSpaces = $state(false)
    let loadingAssignments = $state(false)

    let error: string | null = $state(null)

    let spaceRequestId = 0
    let assignmentRequestId = 0

    const availableEquipment = $derived.by(() => {
        if (!selectedMovementId) {
            return equipment
        }

        const assignedIds = new Set(
            movementAssignments.map(
                assignment => assignment.equipmentId
            )
        )

        return equipment.filter(
            item => assignedIds.has(item.id)
        )
    })

    const selectedEquipment = $derived(
        availableEquipment.find(
            item => item.id === selectedEquipmentId
        )
    )

    const selectedCargoSpace = $derived(
        spaces.find(
            item => item.id === selectedCargoSpaceId
        )
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

    async function handleMovementSelection(
        movementId: string | null
    ): Promise<void> {
        const requestId = ++assignmentRequestId

        selectedMovementId = movementId
        selectedMovementReference = null
        movementAssignments = []

        void selectEquipment('')

        if (!movementId) {
            loadingAssignments = false
            return
        }

        loadingAssignments = true

        try {
            const [movement, assignments] = await Promise.all([
                getMovement(movementId),
                getMovementEquipment(movementId)
            ])

            if (requestId !== assignmentRequestId) return

            selectedMovementReference = movement.reference
            movementAssignments = assignments
        } catch (err) {
            if (requestId !== assignmentRequestId) return

            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve movement details'
        } finally {
            if (requestId === assignmentRequestId) {
                loadingAssignments = false
            }
        }
    }

    function handleMovementCreated(): void {
        assignmentRequestId++

        selectedMovementId = null
        selectedMovementReference = null
        movementAssignments = []
        loadingAssignments = false

        void selectEquipment('')

        movementRefreshKey++
    }

    function openWorkspace(): void {
        if (!selectedEquipment || !selectedCargoSpace) return

        if (selectedMovementId && loadingAssignments) return

        if (selectedCargoSpace.usableHeightMm === null) {
            error =
                'A defined cargo-space height is required for the 3D preview.'
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
            movementId={selectedMovementId}
            movementReference={selectedMovementReference}
            equipmentName={selectedEquipment.name}
            cargoSpaceId={selectedCargoSpace.id}
            cargoSpaceName={selectedCargoSpace.name}
            lengthMm={selectedCargoSpace.usableLengthMm}
            widthMm={selectedCargoSpace.usableWidthMm}
            heightMm={selectedCargoSpace.usableHeightMm}
            equipmentId={selectedEquipment.id}
            onBack={() => workspaceOpen = false}
    />

{:else}

    <div class="space-y-6">

        <!-- Movement Creation -->

        <MovementCreationForm
                onCreated={handleMovementCreated}
        />

        <hr class="border-slate-800" />

        <!-- Movement List and Assignments -->

        {#key movementRefreshKey}
            <MovementList
                    onSelectionChange={(movementId) => {
                    void handleMovementSelection(movementId)
                }}
            />
        {/key}

        <hr class="border-slate-800" />

        <!-- Movement Planning -->

        <div>
            <h3 class="text-xl font-semibold text-white">
                Movement Planning
            </h3>

            <p class="mt-2 text-sm text-slate-400">
                Select equipment and a cargo space to begin planning a load.
            </p>

            {#if selectedMovementId}
                <p class="mt-2 text-sm text-emerald-400">
                    Planning for
                    {selectedMovementReference ?? 'selected movement'}
                </p>
            {:else}
                <p class="mt-2 text-sm text-slate-500">
                    Independent loading workspace
                </p>
            {/if}
        </div>

        <!-- Errors -->

        {#if error}
            <div
                    role="alert"
                    class="rounded-lg border border-red-800
                       bg-red-950/40 p-3 text-sm text-red-300"
            >
                {error}
            </div>
        {/if}

        <!-- Equipment and Cargo Spaces -->

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
                        disabled={loadingEquipment || loadingAssignments}
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2.5 text-sm text-white"
                >
                    <option value="">
                        {loadingEquipment
                            ? 'Loading equipment...'
                            : loadingAssignments
                                ? 'Loading assigned equipment...'
                                : 'Select equipment...'}
                    </option>

                    {#each availableEquipment as item (item.id)}
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
                        disabled={
                        !selectedEquipmentId ||
                        loadingSpaces ||
                        loadingAssignments
                    }
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

        <!-- No Assigned Equipment -->

        {#if selectedMovementId &&
        !loadingAssignments &&
        availableEquipment.length === 0 &&
        !error}

            <p class="text-sm text-amber-400">
                No equipment is assigned to this movement.
                Assign equipment above before planning a load.
            </p>
        {/if}

        <!-- No Cargo Spaces -->

        {#if selectedEquipmentId &&
        !loadingSpaces &&
        spaces.length === 0}

            <p class="text-sm text-amber-400">
                This equipment has no registered cargo spaces.
                Add one from Fleet before planning a load.
            </p>
        {/if}

        <!-- Selected Cargo Space -->

        {#if selectedCargoSpace}
            <div
                    class="rounded-xl border border-slate-800
                       bg-slate-950 p-4 text-sm"
            >
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

        <!-- Open Workspace -->

        <div class="flex justify-end">
            <button
                    type="button"
                    onclick={openWorkspace}
                    disabled={
                    !selectedEquipment ||
                    !selectedCargoSpace ||
                    loadingAssignments ||
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
