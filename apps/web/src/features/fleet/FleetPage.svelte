
<script lang="ts">
    import { onMount } from 'svelte'
    import { getEquipment, registerEquipment, type EquipmentRegistrationData } from '../../lib/api/equipment'
    import {
        categoryLabels,
        type Equipment, type EquipmentCategory,
    } from '../../lib/models/equipment'
    import EquipmentRegistrationForm from './EquipmentRegistrationForm.svelte'

    let showRegistrationForm = $state(false)
    let equipment: Equipment[] = $state([])
    let loading = $state(true)
    let error: string | null = $state(null)

    async function loadEquipment(): Promise<void> {
        loading = true
        error = null

        try {
            equipment = await getEquipment()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve equipment'
        } finally {
            loading = false
        }
    }


    async function handleRegistration(
        data: EquipmentRegistrationData
    ): Promise<void> {
        try {
            const created = await registerEquipment(data)

            equipment = [...equipment, created].sort((a, b) =>
                a.name.localeCompare(b.name)
            )

            showRegistrationForm = false
            error = null
        } catch (err) {
            console.error('Equipment registration failed:', err)
            alert(
                err instanceof Error
                    ? err.message
                    : 'Unable to register equipment'
            )
        }
    }


    onMount(() => {
        void loadEquipment()
    })
</script>



{#if !showRegistrationForm}
    <div class="mb-6 flex justify-end">
        <button
                type="button"
                onclick={() => showRegistrationForm = true}
                class="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-emerald-500"
        >
            + Register Equipment
        </button>
    </div>
{/if}


{#if showRegistrationForm}
    <div class="mb-6">
        <EquipmentRegistrationForm
                onCancel={() => showRegistrationForm = false}
                onSubmit={handleRegistration}
        />
    </div>
{/if}

<div class="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
    <div class="flex items-center justify-between border-b border-slate-800 p-6">
        <div>
            <h3 class="text-lg font-semibold">Equipment</h3>
            <p class="mt-1 text-sm text-slate-400">
                Vehicles, trailers, and operational equipment
            </p>
        </div>

        <span class="rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-300">
      {equipment.length} Assets
    </span>
    </div>

    {#if loading}
        <div class="p-8 text-center text-slate-400">
            Loading equipment...
        </div>
    {:else if error}
        <div class="p-8 text-center">
            <p class="text-red-400">{error}</p>

            <button
                    type="button"
                    onclick={loadEquipment}
                    class="mt-4 rounded-lg bg-slate-800 px-4 py-2 text-sm text-white hover:bg-slate-700"
            >
                Retry
            </button>
        </div>
    {:else if equipment.length === 0}
        <div class="p-8 text-center text-slate-400">
            No equipment registered yet.
        </div>
    {:else}
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead class="bg-slate-800/50 text-xs uppercase tracking-wide text-slate-400">
                <tr>
                    <th class="px-6 py-4 font-medium">Name</th>
                    <th class="px-6 py-4 font-medium">Category</th>
                    <th class="px-6 py-4 font-medium">VIN</th>
                </tr>
                </thead>

                <tbody class="divide-y divide-slate-800">
                {#each equipment as item (item.id)}
                    <tr class="transition-colors hover:bg-slate-800/40">
                        <td class="px-6 py-4 font-medium text-white">
                            {item.name}
                        </td>

                        <td class="px-6 py-4 text-slate-300">
                            {categoryLabels[item.category]}
                        </td>

                        <td class="px-6 py-4 text-slate-400">
                            {item.vin ?? '—'}
                        </td>
                    </tr>
                {/each}
                </tbody>
            </table>
        </div>
    {/if}
</div>
