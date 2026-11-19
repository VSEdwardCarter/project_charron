
<script lang="ts">
    import {
        categoryLabels,
        type Equipment,
    } from '../../lib/models/equipment'

    interface Props {
        equipment: Equipment
        onBack: () => void
    }
    import { onMount } from 'svelte'
    import {
        getEquipmentSpecifications,
        type EquipmentSpecifications,
    } from '../../lib/api/equipmentSpecifications'

    let specifications: EquipmentSpecifications | null = $state(null)
    let specificationsLoading = $state(true)
    let specificationsError: string | null = $state(null)
    let { equipment, onBack }: Props = $props()

    onMount(() => {
        void loadSpecifications()
    })

    async function loadSpecifications(): Promise<void> {
        specificationsLoading = true
        specificationsError = null

        try {
            specifications = await getEquipmentSpecifications(equipment.id)
        } catch (err) {
            specificationsError = err instanceof Error
                ? err.message
                : 'Unable to retrieve equipment specifications'
        } finally {
            specificationsLoading = false
        }
    }

    function formatMillimeters(value: number | null): string {
        return value === null
            ? 'Not recorded'
            : `${(value / 1000).toFixed(2)} m`
    }

    function formatGrams(value: number | null): string {
        return value === null
            ? 'Not recorded'
            : `${(value / 1000).toLocaleString()} kg`
    }
</script>

<div class="rounded-xl border border-slate-800 bg-slate-900 p-6">
    <button
            type="button"
            onclick={onBack}
            class="mb-6 text-sm text-slate-400 hover:text-white"
    >
        ← Back to Fleet
    </button>

    <div class="mb-6">
        <h3 class="text-xl font-semibold text-white">
            {equipment.name}
        </h3>

        <p class="mt-1 text-sm text-slate-400">
            Equipment Details
        </p>
    </div>

    <dl class="grid gap-6 sm:grid-cols-2">
        <div>
            <dt class="text-sm text-slate-400">Name</dt>
            <dd class="mt-1 text-white">{equipment.name}</dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">Category</dt>
            <dd class="mt-1 text-white">
                {categoryLabels[equipment.category]}
            </dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">VIN</dt>
            <dd class="mt-1 text-white">
                {equipment.vin ?? 'Not provided'}
            </dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">Equipment ID</dt>
            <dd class="mt-1 break-all font-mono text-sm text-white">
                {equipment.id}
            </dd>
        </div>
    </dl>

    <div class="mt-8 border-t border-slate-800 pt-6">
        <h4 class="mb-4 text-lg font-semibold text-white">
            Equipment Specifications
        </h4>

        {#if specificationsLoading}
            <p class="text-sm text-slate-400">
                Loading specifications...
            </p>
        {:else if specificationsError}
            <div class="flex items-center gap-4">
                <p class="text-sm text-red-400">
                    {specificationsError}
                </p>

                <button
                        type="button"
                        onclick={loadSpecifications}
                        class="text-sm text-emerald-400 hover:underline"
                >
                    Retry
                </button>
            </div>
        {:else if specifications === null}
            <p class="text-sm text-slate-400">
                No specifications recorded for this equipment.
            </p>
        {:else}
            <div class="grid gap-8 md:grid-cols-2">
                <section>
                    <h5 class="mb-3 font-medium text-slate-200">
                        Exterior Dimensions
                    </h5>

                    <dl class="space-y-3 text-sm">
                        <div>
                            <dt class="text-slate-400">Length</dt>
                            <dd class="text-white">
                                {formatMillimeters(specifications.lengthMm)}
                            </dd>
                        </div>

                        <div>
                            <dt class="text-slate-400">Width</dt>
                            <dd class="text-white">
                                {formatMillimeters(specifications.widthMm)}
                            </dd>
                        </div>

                        <div>
                            <dt class="text-slate-400">Height</dt>
                            <dd class="text-white">
                                {formatMillimeters(specifications.heightMm)}
                            </dd>
                        </div>
                    </dl>
                </section>

                <section>
                    <h5 class="mb-3 font-medium text-slate-200">
                        Weight Specifications
                    </h5>

                    {#if !specifications.weightSpecificationsPresent}
                        <p class="text-sm text-slate-400">
                            Weight specifications not recorded.
                        </p>
                    {:else}
                        <dl class="space-y-3 text-sm">
                            <div>
                                <dt class="text-slate-400">Empty Weight</dt>
                                <dd class="text-white">
                                    {formatGrams(specifications.emptyWeightGrams)}
                                </dd>
                            </div>

                            <div>
                                <dt class="text-slate-400">Gross Weight Rating</dt>
                                <dd class="text-white">
                                    {formatGrams(specifications.grossWeightRatingGrams)}
                                </dd>
                            </div>

                            <div>
                                <dt class="text-slate-400">Rated Payload Capacity</dt>
                                <dd class="text-white">
                                    {formatGrams(specifications.ratedPayloadCapacityGrams)}
                                </dd>
                            </div>
                        </dl>
                    {/if}
                </section>
            </div>
        {/if}
    </div>
</div>
