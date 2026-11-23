
<script lang="ts">
    import { onMount } from 'svelte'
    import {
        getMovements,
        type Movement
    } from '../../lib/api/movements'
    import MovementEquipmentAssignment from './MovementEquipmentAssignment.svelte'
    let movements: Movement[] = $state([])
    let loading = $state(true)
    let error: string | null = $state(null)
    let selectedMovementId = $state<string | null>(null)

    interface Props {
        onSelectionChange?: (movementId: string | null) => void
    }

    let { onSelectionChange }: Props = $props()

    function selectMovement(movementId: string): void {
        selectedMovementId =
            selectedMovementId === movementId
                ? null
                : movementId

        onSelectionChange?.(selectedMovementId)
    }


    async function loadMovements(): Promise<void> {
        loading = true
        error = null

        try {
            movements = await getMovements()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve movements'
        } finally {
            loading = false
        }
    }

    onMount(() => {
        void loadMovements()
    })
</script>

<section class="space-y-4">
    <div>
        <h3 class="text-lg font-semibold text-white">
            Planned Movements
        </h3>
        <p class="mt-1 text-sm text-slate-400">
            Transportation movements registered in CHARRON.
        </p>
    </div>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading movements...
        </p>
    {:else if error}
        <p role="alert" class="text-sm text-red-400">
            {error}
        </p>
    {:else if movements.length === 0}
        <p class="rounded-lg border border-slate-800 p-4 text-sm text-slate-400">
            No movements have been registered.
        </p>
    {:else}
        <div class="overflow-x-auto rounded-lg border border-slate-800">
            <table class="w-full text-left text-sm">
                <thead class="bg-slate-900 text-slate-400">
                <tr>
                    <th class="p-3">Reference</th>
                    <th class="p-3">Origin</th>
                    <th class="p-3">Destination</th>
                    <th class="p-3">Departure</th>
                    <th class="p-3">Status</th>
                </tr>
                </thead>
                <tbody>
                {#each movements as movement (movement.id)}
                    <tr class="border-t border-slate-800 text-slate-200">

                        <td class="p-3 font-medium">
                            <button
                                    type="button"
                                    onclick={() => selectMovement(movement.id)}
                                    aria-expanded={selectedMovementId === movement.id}
                                    class="text-emerald-400 hover:text-emerald-300 hover:underline"
                            >
                                {movement.reference}
                            </button>
                        </td>

                        <td class="p-3">{movement.origin}</td>
                        <td class="p-3">{movement.destination}</td>
                        <td class="p-3">
                            {new Date(movement.plannedDeparture).toLocaleString()}
                        </td>
                        <td class="p-3">{movement.status}</td>
                    </tr>
                {/each}
                </tbody>
            </table>

            {#if selectedMovementId}
                {@const selectedMovement = movements.find(
                    movement => movement.id === selectedMovementId
                )}

                {#if selectedMovement}
                    <div class="mt-6 border-t border-slate-800 pt-6">
                        <div class="mb-4">
                            <h3 class="text-lg font-semibold text-white">
                                Equipment Assignments — {selectedMovement.reference}
                            </h3>
                            <p class="mt-1 text-sm text-slate-400">
                                {selectedMovement.origin}
                                →
                                {selectedMovement.destination}
                            </p>
                        </div>

                        {#key selectedMovementId}
                            <MovementEquipmentAssignment
                                    movementId={selectedMovementId}
                            />
                        {/key}
                    </div>
                {/if}
            {/if}

        </div>
    {/if}
</section>
