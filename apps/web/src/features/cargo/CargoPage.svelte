
<script lang="ts">
    import { onMount } from 'svelte'
    import {
        getCargo,
        registerCargo,
        type Cargo,
        type CargoRegistrationData
    } from '../../lib/api/cargo'
    import CargoRegistrationForm from './CargoRegistrationForm.svelte'

    let cargo: Cargo[] = $state([])
    let loading = $state(true)
    let error: string | null = $state(null)
    let showRegistrationForm = $state(false)

    async function loadCargo(): Promise<void> {
        loading = true
        error = null

        try {
            cargo = await getCargo()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve cargo'
        } finally {
            loading = false
        }
    }

    function formatDimensions(item: Cargo): string {
        if (
            item.lengthMm === null ||
            item.widthMm === null ||
            item.heightMm === null
        ) {
            return 'Not specified'
        }

        return `${item.lengthMm} × ${item.widthMm} × ${item.heightMm} mm`
    }

    function formatWeight(grams: number | null): string {
        if (grams === null) {
            return 'Not specified'
        }

        return `${(grams / 1000).toLocaleString()} kg`
    }

    async function handleRegistration(
        data: CargoRegistrationData
    ): Promise<void> {
        const created = await registerCargo(data)

        cargo = [...cargo, created].sort((a, b) =>
            a.name.localeCompare(b.name)
        )

        showRegistrationForm = false
        error = null
    }

    onMount(() => {
        void loadCargo()
    })
</script>

<div class="space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
            <h3 class="text-lg font-semibold text-white">Cargo</h3>
            <p class="mt-1 text-sm text-slate-400">
                Manage reusable cargo definitions and physical measurements.
            </p>
        </div>


        {#if !showRegistrationForm}
            <button
                    type="button"
                    onclick={() => showRegistrationForm = true}
                    class="rounded-lg bg-emerald-600 px-4 py-2.5
           text-sm font-medium text-white
           hover:bg-emerald-500"
            >
                + Register Cargo
            </button>
        {/if}

    </div>


    {#if showRegistrationForm}
        <CargoRegistrationForm
                onSubmit={handleRegistration}
                onCancel={() => showRegistrationForm = false}
        />
    {/if}


    <div class="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
        <div class="flex items-center justify-between border-b border-slate-800 p-6">
            <div>
                <h4 class="font-semibold text-white">Cargo Catalog</h4>
                <p class="mt-1 text-sm text-slate-400">
                    Registered cargo definitions
                </p>
            </div>

            <span class="rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-300">
        {cargo.length} Items
      </span>
        </div>

        {#if loading}
            <div class="p-8 text-center text-slate-400">
                Loading cargo...
            </div>
        {:else if error}
            <div class="p-8 text-center">
                <p class="text-sm text-red-400">{error}</p>
                <button
                        type="button"
                        onclick={loadCargo}
                        class="mt-4 rounded-lg bg-slate-800 px-4 py-2
                 text-sm text-white hover:bg-slate-700"
                >
                    Retry
                </button>
            </div>
        {:else if cargo.length === 0}
            <div class="p-8 text-center text-slate-400">
                No cargo definitions registered yet.
            </div>
        {:else}
            <div class="overflow-x-auto">
                <table class="w-full text-left text-sm">
                    <thead class="bg-slate-800/50 text-xs uppercase text-slate-400">
                    <tr>
                        <th class="px-6 py-4 font-medium">Name</th>
                        <th class="px-6 py-4 font-medium">Reference</th>
                        <th class="px-6 py-4 font-medium">Weight</th>
                        <th class="px-6 py-4 font-medium">Dimensions</th>
                    </tr>
                    </thead>

                    <tbody class="divide-y divide-slate-800">
                    {#each cargo as item (item.id)}
                        <tr class="hover:bg-slate-800/30">
                            <td class="px-6 py-4 font-medium text-white">
                                {item.name}
                            </td>
                            <td class="px-6 py-4 text-slate-300">
                                {item.reference ?? '—'}
                            </td>
                            <td class="px-6 py-4 text-slate-300">
                                {formatWeight(item.weightGrams)}
                            </td>
                            <td class="px-6 py-4 text-slate-300">
                                {formatDimensions(item)}
                            </td>
                        </tr>
                    {/each}
                    </tbody>
                </table>
            </div>
        {/if}
    </div>
</div>
