
<script lang="ts">
    import {
        categoryLabels,
        type Equipment,
    } from '../../lib/models/equipment'
    import EquipmentConfigurations from './EquipmentConfigurations.svelte'
    interface Props {
        equipment: Equipment
        onBack: () => void
    }
    import { onMount } from 'svelte'

    import {
        getEquipmentSpecifications,
        saveEquipmentSpecifications,
        type EquipmentSpecifications,
        type EquipmentSpecificationsInput,
    } from '../../lib/api/equipmentSpecifications'
    import EquipmentCapabilities from './EquipmentCapabilities.svelte'

    import EquipmentSpecificationsForm from './EquipmentSpecificationsForm.svelte'

    let specifications: EquipmentSpecifications | null = $state(null)
    let specificationsLoading = $state(true)
    let specificationsError: string | null = $state(null)
    let { equipment, onBack }: Props = $props()

    let editingSpecifications = $state(false)

    async function handleSaveSpecifications(
        data: EquipmentSpecificationsInput
    ): Promise<void> {
        const saved = await saveEquipmentSpecifications(equipment.id, data)
        console.log('Equipment ID:', equipment.id)
        console.log('Loaded specifications:', specifications)

        specifications = saved
        specificationsError = null
        editingSpecifications = false
    }


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


<div class="mt-8 border-t border-slate-800 pt-6">
    <div class="mb-5 flex items-center justify-between gap-4">
        <h4 class="text-lg font-semibold text-white">
            Equipment Specifications
        </h4>

        {#if !editingSpecifications && !specificationsLoading && !specificationsError}
            <button
                    type="button"
                    onclick={() => editingSpecifications = true}
                    class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
            >
                {specifications ? 'Edit Specifications' : '+ Add Specifications'}
            </button>
        {/if}
    </div>

    {#if editingSpecifications}
        <EquipmentSpecificationsForm
                {specifications}
                onCancel={() => editingSpecifications = false}
                onSave={handleSaveSpecifications}
        />
    {:else if specificationsLoading}
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
            <section>

                <EquipmentCapabilities equipmentId={equipment.id} />
                <EquipmentConfigurations equipmentId={equipment.id} />
            </section>
        </div>
    {/if}
</div>

