
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getMovementEquipment,
        assignMovementEquipment,
        type MovementEquipmentAssignment,
        type MovementEquipmentRole
    } from '../../lib/api/movementEquipment'

    interface Equipment {
        id: string
        name: string
        category: string
        vin: string | null
    }

    interface Props {
        movementId: string
    }

    let { movementId }: Props = $props()

    let equipment: Equipment[] = $state([])
    let assignments: MovementEquipmentAssignment[] = $state([])

    let selectedEquipmentId = $state('')
    let selectedRole: MovementEquipmentRole = $state('POWER_UNIT')

    let loading = $state(true)
    let submitting = $state(false)
    let error: string | null = $state(null)
    let success: string | null = $state(null)

    async function loadData(): Promise<void> {
        loading = true
        error = null

        try {
            const equipmentResponse = await fetch('/api/equipment')

            if (!equipmentResponse.ok) {
                throw new Error('Unable to retrieve fleet equipment')
            }

            const [fleet, movementAssignments] = await Promise.all([
                equipmentResponse.json() as Promise<Equipment[]>,
                getMovementEquipment(movementId)
            ])

            equipment = fleet
            assignments = movementAssignments
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to load equipment assignments'
        } finally {
            loading = false
        }
    }

    async function assignEquipment(): Promise<void> {
        if (!selectedEquipmentId || submitting) return

        submitting = true
        error = null
        success = null

        try {
            await assignMovementEquipment(movementId, {
                equipmentId: selectedEquipmentId,
                role: selectedRole
            })

            assignments = await getMovementEquipment(movementId)

            const selected = equipment.find(
                item => item.id === selectedEquipmentId
            )

            success = `${selected?.name ?? 'Equipment'} assigned successfully.`

            selectedEquipmentId = ''
            selectedRole = 'POWER_UNIT'
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to assign equipment'
        } finally {
            submitting = false
        }
    }

    function equipmentName(equipmentId: string): string {
        return equipment.find(item => item.id === equipmentId)?.name
            ?? equipmentId
    }

    onMount(() => {
        void loadData()
    })
</script>

<section class="space-y-4">
    <div>
        <h3 class="text-lg font-semibold text-white">
            Movement Equipment
        </h3>

        <p class="mt-1 text-sm text-slate-400">
            Assign registered fleet equipment to this movement.
        </p>
    </div>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading equipment...
        </p>
    {:else}
        <div class="space-y-4 rounded-xl border border-slate-800 p-5">

            <div class="grid gap-4 md:grid-cols-2">
                <div class="space-y-2">
                    <label
                            for="assignment-equipment"
                            class="block text-sm text-slate-300"
                    >
                        Equipment
                    </label>

                    <select
                            id="assignment-equipment"
                            bind:value={selectedEquipmentId}
                            class="w-full rounded-lg border border-slate-700
                               bg-slate-950 px-3 py-2 text-white"
                    >
                        <option value="">Select equipment...</option>

                        {#each equipment as item (item.id)}
                            <option value={item.id}>
                                {item.name} ({item.category})
                            </option>
                        {/each}
                    </select>
                </div>

                <div class="space-y-2">
                    <label
                            for="assignment-role"
                            class="block text-sm text-slate-300"
                    >
                        Assignment Role
                    </label>

                    <select
                            id="assignment-role"
                            bind:value={selectedRole}
                            class="w-full rounded-lg border border-slate-700
                               bg-slate-950 px-3 py-2 text-white"
                    >
                        <option value="POWER_UNIT">Power Unit</option>
                        <option value="CARGO_CARRIER">Cargo Carrier</option>
                        <option value="SUPPORT">Support</option>
                    </select>
                </div>
            </div>

            {#if error}
                <p role="alert" class="text-sm text-red-400">
                    {error}
                </p>
            {/if}

            {#if success}
                <p role="status" class="text-sm text-emerald-400">
                    {success}
                </p>
            {/if}

            <div class="flex justify-end">
                <button
                        type="button"
                        onclick={assignEquipment}
                        disabled={!selectedEquipmentId || submitting}
                        class="rounded-lg bg-emerald-600 px-5 py-2.5
                           text-sm font-medium text-white
                           hover:bg-emerald-500
                           disabled:cursor-not-allowed
                           disabled:opacity-50"
                >
                    {submitting ? 'Assigning...' : 'Assign Equipment'}
                </button>
            </div>
        </div>

        <div class="space-y-2">
            <h4 class="text-sm font-semibold text-white">
                Assigned Equipment
            </h4>

            {#if assignments.length === 0}
                <p class="text-sm text-slate-400">
                    No equipment assigned to this movement.
                </p>
            {:else}
                <div class="overflow-x-auto rounded-lg border border-slate-800">
                    <table class="w-full text-left text-sm">
                        <thead class="bg-slate-900 text-slate-400">
                        <tr>
                            <th class="p-3">Equipment</th>
                            <th class="p-3">Role</th>
                            <th class="p-3">Assigned</th>
                        </tr>
                        </thead>
                        <tbody>
                        {#each assignments as assignment (assignment.id)}
                            <tr class="border-t border-slate-800 text-slate-200">
                                <td class="p-3">
                                    {equipmentName(assignment.equipmentId)}
                                </td>
                                <td class="p-3">
                                    {assignment.role.replaceAll('_', ' ')}
                                </td>
                                <td class="p-3">
                                    {new Date(assignment.assignedAt).toLocaleString()}
                                </td>
                            </tr>
                        {/each}
                        </tbody>
                    </table>
                </div>
            {/if}
        </div>
    {/if}
</section>
