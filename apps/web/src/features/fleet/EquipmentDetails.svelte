
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        categoryLabels,
        type Equipment,
    } from '../../lib/models/equipment'
    import EquipmentModifications from './EquipmentModifications.svelte'
    import {
        getEquipmentSpecifications,
        saveEquipmentSpecifications,
        type EquipmentSpecifications,
        type EquipmentSpecificationsInput,
    } from '../../lib/api/equipmentSpecifications'

    import {
        getLatestVinDecode,
        type VinDecodeSnapshot,
    } from '../../lib/api/equipmentVinDecodes'

    import EquipmentSpecificationsForm from './EquipmentSpecificationsForm.svelte'
    import EquipmentCapabilities from './EquipmentCapabilities.svelte'
    import EquipmentConfigurations from './EquipmentConfigurations.svelte'
    import EquipmentVinDetails from './EquipmentVinDetails.svelte'

    interface Props {
        equipment: Equipment
        onBack: () => void
    }

    type EquipmentTab =
        | 'Overview'
        | 'Specifications'
        | 'Capabilities'
        | 'Configurations'
        | 'Modifications'
        | 'VIN Decode'

    const tabs: EquipmentTab[] = [
        'Overview',
        'Specifications',
        'Capabilities',
        'Configurations',
        'Modifications',
        'VIN Decode',
    ]

    let { equipment, onBack }: Props = $props()

    let activeTab = $state<EquipmentTab>('Overview')

    let specifications = $state<EquipmentSpecifications | null>(null)
    let specificationsLoading = $state(true)
    let specificationsError = $state<string | null>(null)
    let editingSpecifications = $state(false)

    let vinSnapshot = $state<VinDecodeSnapshot | null>(null)
    let vinLoading = $state(true)
    let vinError = $state<string | null>(null)

    const vehicle = $derived(vinSnapshot?.decodedVehicle ?? null)

    onMount(() => {
        void loadSpecifications()
        void loadVinOverview()
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

    async function loadVinOverview(): Promise<void> {
        vinLoading = true
        vinError = null

        try {
            vinSnapshot = await getLatestVinDecode(equipment.id)
        } catch (err) {
            vinError = err instanceof Error
                ? err.message
                : 'Unable to retrieve vehicle information'
        } finally {
            vinLoading = false
        }
    }

    async function handleSaveSpecifications(
        data: EquipmentSpecificationsInput
    ): Promise<void> {
        const saved = await saveEquipmentSpecifications(
            equipment.id,
            data
        )

        specifications = saved
        specificationsError = null
        editingSpecifications = false
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

    function formatValue(
        value: string | number | null | undefined
    ): string {
        return value === null || value === undefined || value === ''
            ? 'Unknown'
            : String(value)
    }
</script>

<div class="space-y-6">

    <!-- Equipment target card header -->
    <header class="rounded-xl border border-slate-800 bg-slate-900 p-6">

        <button
                type="button"
                onclick={onBack}
                class="mb-6 inline-flex items-center gap-2 text-sm text-emerald-400 hover:text-emerald-300"
        >
            <span aria-hidden="true">←</span>
            Back to Fleet
        </button>

        <div class="flex flex-wrap items-start justify-between gap-4">

            <div class="flex items-start gap-4">

                <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-slate-800 text-2xl">
                    🚛
                </div>

                <div>
                    <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Equipment Profile
                    </p>

                    <h2 class="text-2xl font-semibold text-white">
                        {equipment.name}
                    </h2>

                    <p class="mt-2 break-all text-sm text-slate-400">
                        VIN: {equipment.vin ?? 'Not recorded'}
                    </p>

                    {#if vehicle}
                        <p class="mt-2 text-sm text-slate-300">
                            {formatValue(vehicle.modelYear)}
                            {formatValue(vehicle.make)}
                            {formatValue(vehicle.model)}
                            {vehicle.trim ?? ''}
                        </p>
                    {/if}
                </div>
            </div>

            <span class="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-slate-200">
                {categoryLabels[equipment.category]}
            </span>
        </div>

        <!-- Workspace navigation -->
        <nav
                aria-label="Equipment sections"
                class="mt-7 flex flex-wrap gap-2 border-t border-slate-800 pt-5"
        >
            {#each tabs as tab}
                <button
                        type="button"
                        onclick={() => activeTab = tab}
                        aria-pressed={activeTab === tab}
                        class={activeTab === tab
                        ? 'rounded-full bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white'
                        : 'rounded-full border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs text-slate-300 hover:bg-slate-800 hover:text-white'}
                >
                    {tab}
                </button>
            {/each}
        </nav>
    </header>

    <!-- Selected equipment section -->
    <main class="rounded-xl border border-slate-800 bg-slate-900 p-6">

        {#if activeTab === 'Overview'}

            <div class="mb-6">
                <h3 class="text-lg font-semibold text-white">
                    Equipment Overview
                </h3>
                <p class="mt-1 text-sm text-slate-400">
                    Identity and currently recorded equipment information.
                </p>
            </div>

            <div class="grid gap-4 md:grid-cols-2">

                <section class="rounded-lg border border-slate-800 bg-slate-950 p-5">
                    <h4 class="mb-4 font-medium text-white">
                        Asset Identity
                    </h4>

                    <dl class="space-y-4 text-sm">
                        <div>
                            <dt class="text-slate-400">Equipment Name</dt>
                            <dd class="mt-1 text-white">{equipment.name}</dd>
                        </div>
                        <div>
                            <dt class="text-slate-400">Category</dt>
                            <dd class="mt-1 text-white">
                                {categoryLabels[equipment.category]}
                            </dd>
                        </div>
                        <div>
                            <dt class="text-slate-400">VIN</dt>
                            <dd class="mt-1 break-all text-white">
                                {equipment.vin ?? 'Not recorded'}
                            </dd>
                        </div>
                    </dl>
                </section>

                <section class="rounded-lg border border-slate-800 bg-slate-950 p-5">
                    <h4 class="mb-4 font-medium text-white">
                        Vehicle Summary
                    </h4>

                    {#if vinLoading}
                        <p class="text-sm text-slate-400">
                            Loading vehicle information...
                        </p>
                    {:else if vinError}
                        <p class="text-sm text-red-400">{vinError}</p>
                        <button
                                type="button"
                                onclick={loadVinOverview}
                                class="mt-3 text-sm text-emerald-400 hover:underline"
                        >
                            Retry
                        </button>
                    {:else if vehicle}
                        <dl class="space-y-4 text-sm">
                            <div>
                                <dt class="text-slate-400">Manufacturer</dt>
                                <dd class="mt-1 text-white">
                                    {formatValue(vehicle.manufacturer)}
                                </dd>
                            </div>
                            <div>
                                <dt class="text-slate-400">Year / Make / Model</dt>
                                <dd class="mt-1 text-white">
                                    {formatValue(vehicle.modelYear)}
                                    {formatValue(vehicle.make)}
                                    {formatValue(vehicle.model)}
                                </dd>
                            </div>
                            <div>
                                <dt class="text-slate-400">Engine</dt>
                                <dd class="mt-1 text-white">
                                    {formatValue(vehicle.displacementLiters)} L
                                    /
                                    {formatValue(vehicle.engineCylinders)} cylinders
                                </dd>
                            </div>
                            <div>
                                <dt class="text-slate-400">Fuel / Drive Type</dt>
                                <dd class="mt-1 text-white">
                                    {formatValue(vehicle.fuelType)}
                                    /
                                    {formatValue(vehicle.driveType)}
                                </dd>
                            </div>
                        </dl>

                        <button
                                type="button"
                                onclick={() => activeTab = 'VIN Decode'}
                                class="mt-5 text-sm font-medium text-emerald-400 hover:underline"
                        >
                            View VIN Information →
                        </button>
                    {:else}
                        <p class="text-sm text-slate-400">
                            No VIN decoding information recorded.
                        </p>
                    {/if}
                </section>

                <section class="rounded-lg border border-slate-800 bg-slate-950 p-5 md:col-span-2">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h4 class="font-medium text-white">
                                Operational Specifications
                            </h4>

                            <p class="mt-2 text-sm text-slate-400">
                                {specificationsLoading
                                    ? 'Loading specifications...'
                                    : specificationsError
                                        ? 'Unable to load specifications.'
                                        : specifications
                                            ? 'Equipment specifications are recorded.'
                                            : 'No physical specifications recorded yet.'}
                            </p>
                        </div>

                        <button
                                type="button"
                                onclick={() => activeTab = 'Specifications'}
                                class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-emerald-400 hover:bg-slate-800"
                        >
                            View Specifications →
                        </button>
                    </div>
                </section>

            </div>

        {:else if activeTab === 'Specifications'}

            <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
                <h3 class="text-lg font-semibold text-white">
                    Equipment Specifications
                </h3>

                {#if !editingSpecifications && !specificationsLoading && !specificationsError}
                    <button
                            type="button"
                            onclick={() => editingSpecifications = true}
                            class="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-emerald-500"
                    >
                        {specifications
                            ? 'Edit Specifications'
                            : '+ Add Specifications'}
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
                <p class="text-sm text-red-400">
                    {specificationsError}
                </p>
                <button
                        type="button"
                        onclick={loadSpecifications}
                        class="mt-3 text-sm text-emerald-400 hover:underline"
                >
                    Retry
                </button>

            {:else if specifications === null}
                <p class="text-sm text-slate-400">
                    No specifications recorded for this equipment.
                </p>

            {:else}
                <div class="grid gap-8 md:grid-cols-2">

                    <section>
                        <h4 class="mb-4 font-medium text-slate-200">
                            Exterior Dimensions
                        </h4>

                        <dl class="space-y-4 text-sm">
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
                        <h4 class="mb-4 font-medium text-slate-200">
                            Weight Specifications
                        </h4>

                        {#if !specifications.weightSpecificationsPresent}
                            <p class="text-sm text-slate-400">
                                Weight specifications not recorded.
                            </p>
                        {:else}
                            <dl class="space-y-4 text-sm">
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

        {:else if activeTab === 'Capabilities'}

            <h3 class="mb-6 text-lg font-semibold text-white">
                Equipment Capabilities
            </h3>

            <EquipmentCapabilities equipmentId={equipment.id} />

        {:else if activeTab === 'Configurations'}

            <h3 class="mb-6 text-lg font-semibold text-white">
                Equipment Configurations
            </h3>

            <EquipmentConfigurations equipmentId={equipment.id} />


        {:else if activeTab === 'Modifications'}

            <EquipmentModifications equipmentId={equipment.id} />


        {:else if activeTab === 'VIN Decode'}

            <EquipmentVinDetails equipmentId={equipment.id} />

        {/if}

    </main>
</div>
