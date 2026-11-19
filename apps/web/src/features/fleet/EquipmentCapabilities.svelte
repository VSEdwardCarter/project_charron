
<script lang="ts">
    import { onMount } from 'svelte'
    import {
        equipmentCapabilityLabels,
        getEquipmentCapabilities,
        addEquipmentCapability,
        removeEquipmentCapability,
        type EquipmentCapability,
    } from '../../lib/api/equipmentCapabilities'

    interface Props {
        equipmentId: string
    }

    let { equipmentId }: Props = $props()

    let capabilities: EquipmentCapability[] = $state([])
    let selectedCapability: EquipmentCapability | '' = $state('')
    let loading = $state(true)
    let saving = $state(false)
    let error: string | null = $state(null)

    const availableCapabilities = $derived(
        (Object.keys(equipmentCapabilityLabels) as EquipmentCapability[])
            .filter(capability => !capabilities.includes(capability))
    )

    async function loadCapabilities(): Promise<void> {
        loading = true
        error = null

        try {
            capabilities = await getEquipmentCapabilities(equipmentId)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve capabilities'
        } finally {
            loading = false
        }
    }

    async function handleAdd(): Promise<void> {
        if (!selectedCapability || saving) return

        saving = true
        error = null

        try {
            await addEquipmentCapability(equipmentId, selectedCapability)
            capabilities = [...capabilities, selectedCapability]
            selectedCapability = ''
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to assign capability'
        } finally {
            saving = false
        }
    }

    async function handleRemove(
        capability: EquipmentCapability
    ): Promise<void> {
        if (saving) return

        saving = true
        error = null

        try {
            await removeEquipmentCapability(equipmentId, capability)
            capabilities = capabilities.filter(item => item !== capability)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to remove capability'
        } finally {
            saving = false
        }
    }

    onMount(() => {
        void loadCapabilities()
    })
</script>

<section class="mt-8 border-t border-slate-800 pt-6">
    <h4 class="mb-2 text-lg font-semibold text-white">
        Equipment Capabilities
    </h4>

    <p class="mb-5 text-sm text-slate-400">
        Capabilities belonging to this equipment, independent of its configurations.
    </p>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading capabilities...
        </p>
    {:else if error && capabilities.length === 0}
        <div class="mb-4">
            <p role="alert" class="text-sm text-red-400">{error}</p>
            <button
                    type="button"
                    onclick={loadCapabilities}
                    class="mt-2 text-sm text-emerald-400 hover:underline"
            >
                Retry
            </button>
        </div>
    {:else}
        {#if capabilities.length === 0}
            <p class="mb-5 text-sm text-slate-400">
                No base equipment capabilities recorded.
            </p>
        {:else}
            <div class="mb-5 flex flex-wrap gap-2">
                {#each capabilities as capability (capability)}
                    <div class="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2">
                        <span class="text-sm text-slate-200">
                            {equipmentCapabilityLabels[capability]}
                        </span>

                        <button
                                type="button"
                                disabled={saving}
                                onclick={() => handleRemove(capability)}
                                aria-label={`Remove ${equipmentCapabilityLabels[capability]}`}
                                title="Remove capability"
                                class="text-slate-400 hover:text-red-400 disabled:opacity-50"
                        >
                            ×
                        </button>
                    </div>
                {/each}
            </div>
        {/if}

        {#if availableCapabilities.length > 0}
            <div class="flex flex-wrap items-end gap-3">
                <div class="min-w-52 flex-1">
                    <label for="capability-select" class="mb-2 block text-sm text-slate-300">
                        Add Capability
                    </label>

                    <select
                            id="capability-select"
                            bind:value={selectedCapability}
                            disabled={saving}
                            class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white"
                    >
                        <option value="">Select a capability</option>

                        {#each availableCapabilities as capability}
                            <option value={capability}>
                                {equipmentCapabilityLabels[capability]}
                            </option>
                        {/each}
                    </select>
                </div>

                <button
                        type="button"
                        onclick={handleAdd}
                        disabled={!selectedCapability || saving}
                        class="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {saving ? 'Saving...' : 'Add Capability'}
                </button>
            </div>
        {/if}

        {#if error}
            <p role="alert" class="mt-4 text-sm text-red-400">
                {error}
            </p>
        {/if}
    {/if}
</section>
