
<script lang="ts">
    import { onMount } from 'svelte'
    import {
        getLatestVinDecode,
        type VinDecodeSnapshot,
        type VinDecodeAttribute,
    } from '../../lib/api/equipmentVinDecodes'

    interface Props {
        equipmentId: string
    }

    let { equipmentId }: Props = $props()

    let snapshot = $state<VinDecodeSnapshot | null>(null)
    let loading = $state(true)
    let error = $state<string | null>(null)

    let showAllAttributes = $state(false)

    const groupedAttributes = $derived.by(() => {
        const groups = new Map<string, VinDecodeAttribute[]>()

        for (const attribute of snapshot?.rawResponse ?? []) {
            const group = attribute.groupname?.trim() || 'Ungrouped'
            const existing = groups.get(group) ?? []
            existing.push(attribute)
            groups.set(group, existing)
        }

        return [...groups.entries()].sort(
            ([a], [b]) => a.localeCompare(b)
        )
    })

    async function loadVinDetails(): Promise<void> {
        loading = true
        error = null

        try {
            snapshot = await getLatestVinDecode(equipmentId)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve vehicle information'
        } finally {
            loading = false
        }
    }

    onMount(() => {
        void loadVinDetails()
    })
</script>

<section class="mt-8 border-t border-slate-800 pt-6">
    <h4 class="mb-5 text-lg font-semibold text-white">
        Vehicle Information
    </h4>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading vehicle information...
        </p>

    {:else if error}
        <div class="flex items-center gap-4">
            <p class="text-sm text-red-400" role="alert">
                {error}
            </p>

            <button
                    type="button"
                    onclick={loadVinDetails}
                    class="text-sm text-emerald-400 hover:underline"
            >
                Retry
            </button>
        </div>

    {:else if snapshot === null}
        <p class="text-sm text-slate-400">
            No VIN decoding information recorded for this equipment.
        </p>

    {:else}
        {@const vehicle = snapshot.decodedVehicle}

        <p class="mb-5 text-xs text-slate-400">
            Manufacturer-reference information captured during
            equipment registration. Current vehicle characteristics
            may differ.
        </p>

        <div class="grid gap-6 md:grid-cols-2">
            <section>
                <h5 class="mb-3 font-medium text-slate-200">
                    Vehicle Identity
                </h5>

                <dl class="space-y-3 text-sm">
                    {#each [
                        ['VIN', vehicle.vin],
                        ['Manufacturer', vehicle.manufacturer],
                        ['Make', vehicle.make],
                        ['Model', vehicle.model],
                        ['Year', vehicle.modelYear],
                        ['Trim', vehicle.trim],
                        ['Vehicle Type', vehicle.vehicleType],
                        ['Body Class', vehicle.bodyClass],
                        ['GVWR Class', vehicle.gvwrClass],
                    ] as [string, string | number | null][] as item}
                        <div>
                            <dt class="text-slate-400">{item[0]}</dt>
                            <dd class="text-white">{item[1] ?? 'Unknown'}</dd>
                        </div>
                    {/each}
                </dl>
            </section>

            <section>
                <h5 class="mb-3 font-medium text-slate-200">
                    Engine and Drivetrain
                </h5>

                <dl class="space-y-3 text-sm">
                    {#each [
                        ['Engine Manufacturer', vehicle.engineManufacturer],
                        ['Engine Model', vehicle.engineModel],
                        ['Configuration', vehicle.engineConfiguration],
                        ['Cylinders', vehicle.engineCylinders],
                        ['Displacement (L)', vehicle.displacementLiters],
                        ['Horsepower', vehicle.engineHorsepower],
                        ['Fuel Type', vehicle.fuelType],
                        ['Transmission', vehicle.transmission],
                        ['Drive Type', vehicle.driveType],
                    ] as [string, string | number | null][] as item}
                        <div>
                            <dt class="text-slate-400">{item[0]}</dt>
                            <dd class="text-white">{item[1] ?? 'Unknown'}</dd>
                        </div>
                    {/each}
                </dl>
            </section>
        </div>

        <div class="mt-7 border-t border-slate-800 pt-5">
            <button
                    type="button"
                    onclick={() => showAllAttributes = !showAllAttributes}
                    aria-expanded={showAllAttributes}
                    class="text-sm font-medium text-emerald-400 hover:underline"
            >
                {showAllAttributes ? 'Hide' : 'View'}
                Complete VIN Decode
                ({snapshot.rawResponse.length} attributes)
            </button>

            {#if showAllAttributes}
                <div class="mt-5 space-y-6">
                    {#each groupedAttributes as [group, attributes] (group)}
                        <section>
                            <h6 class="mb-3 font-medium text-slate-200">
                                {group} ({attributes.length})
                            </h6>

                            <div class="space-y-2">
                                {#each attributes as attribute, index (index)}
                                    <div class="rounded-lg border border-slate-800 p-3">
                                        <div class="flex flex-wrap items-start justify-between gap-3">
                                            <div class="min-w-0">
                                                <p class="text-sm text-slate-400">
                                                    {attribute.variable ?? attribute.code ?? 'Unnamed attribute'}
                                                </p>
                                                <p class="mt-1 break-words text-sm text-white">
                                                    {attribute.value === null || attribute.value === ''
                                                        ? 'Unknown / Not provided'
                                                        : attribute.value}
                                                </p>
                                            </div>
                                        </div>

                                        <details class="mt-2">
                                            <summary class="cursor-pointer text-xs text-slate-500">
                                                Source metadata
                                            </summary>

                                            <pre class="mt-2 overflow-x-auto whitespace-pre-wrap break-words text-xs text-slate-400">{JSON.stringify(attribute, null, 2)}</pre>
                                        </details>
                                    </div>
                                {/each}
                            </div>
                        </section>
                    {/each}
                </div>
            {/if}
        </div>
    {/if}
</section>
