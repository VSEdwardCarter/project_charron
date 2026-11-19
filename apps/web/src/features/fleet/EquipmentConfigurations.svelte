
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getEquipmentConfigurations,
        createEquipmentConfiguration,
        updateEquipmentConfiguration,
        getConfigurationCapabilities,
        addConfigurationCapability,
        removeConfigurationCapability,
        EQUIPMENT_CAPABILITIES,
        type EquipmentConfiguration,
        type EquipmentCapability,
    } from '../../lib/api/equipmentConfigurations'


    interface Props {
        equipmentId: string
    }

    let { equipmentId }: Props = $props()

    let configurations: EquipmentConfiguration[] = $state([])
    let loading = $state(true)
    let error: string | null = $state(null)

    let expandedConfigurationId = $state<string | null>(null)
    let configurationCapabilities = $state<EquipmentCapability[]>([])
    let capabilitiesLoading = $state(false)
    let capabilitiesSaving = $state(false)
    let capabilitiesError = $state<string | null>(null)

    async function toggleConfigurationCapabilities(
        configurationId: string
    ): Promise<void> {
        if (expandedConfigurationId === configurationId) {
            expandedConfigurationId = null
            return
        }

        expandedConfigurationId = configurationId
        configurationCapabilities = []
        capabilitiesLoading = true
        capabilitiesError = null

        try {
            const capabilities = await getConfigurationCapabilities(
                equipmentId,
                configurationId
            )

            if (expandedConfigurationId === configurationId) {
                configurationCapabilities = capabilities
            }
        } catch (err) {
            if (expandedConfigurationId === configurationId) {
                capabilitiesError = err instanceof Error
                    ? err.message
                    : 'Unable to retrieve configuration capabilities'
            }
        } finally {
            if (expandedConfigurationId === configurationId) {
                capabilitiesLoading = false
            }
        }
    }

    async function toggleCapability(
        configurationId: string,
        capability: EquipmentCapability
    ): Promise<void> {
        if (capabilitiesSaving) return

        capabilitiesSaving = true
        capabilitiesError = null

        const assigned = configurationCapabilities.includes(capability)

        try {
            if (assigned) {
                await removeConfigurationCapability(
                    equipmentId,
                    configurationId,
                    capability
                )
            } else {
                await addConfigurationCapability(
                    equipmentId,
                    configurationId,
                    capability
                )
            }

            configurationCapabilities = await getConfigurationCapabilities(
                equipmentId,
                configurationId
            )
        } catch (err) {
            capabilitiesError = err instanceof Error
                ? err.message
                : 'Unable to update configuration capabilities'
        } finally {
            capabilitiesSaving = false
        }
    }


    async function loadConfigurations(): Promise<void> {
        loading = true
        error = null

        try {
            configurations = await getEquipmentConfigurations(equipmentId)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve configurations'
        } finally {
            loading = false
        }
    }

    let showCreateForm = $state(false)
    let configurationName = $state('')
    let configurationActive = $state(true)
    let saving = $state(false)
    let saveError: string | null = $state(null)

    async function handleCreate(event: SubmitEvent): Promise<void> {
        event.preventDefault()

        const name = configurationName.trim()

        if (!name || saving) return

        saving = true
        saveError = null

        try {
            const created = await createEquipmentConfiguration(
                equipmentId,
                {
                    name,
                    active: configurationActive,
                }
            )

            configurations = [...configurations, created].sort(
                (a, b) => a.name.localeCompare(b.name)
            )

            configurationName = ''
            configurationActive = true
            showCreateForm = false
        } catch (err) {
            saveError = err instanceof Error
                ? err.message
                : 'Unable to create configuration'
        } finally {
            saving = false
        }
    }


    let editingConfigurationId = $state<string | null>(null)

    function handleEdit(configuration: EquipmentConfiguration): void {
        editingConfigurationId = configuration.id
        configurationName = configuration.name
        configurationActive = configuration.active
        saveError = null
        showCreateForm = false
    }

    function cancelEdit(): void {
        editingConfigurationId = null
        configurationName = ''
        configurationActive = true
        saveError = null
    }

    async function handleUpdate(event: SubmitEvent): Promise<void> {
        event.preventDefault()

        if (!editingConfigurationId || saving) return

        const name = configurationName.trim()
        if (!name) return

        saving = true
        saveError = null

        try {
            const updated = await updateEquipmentConfiguration(
                equipmentId,
                editingConfigurationId,
                {
                    name,
                    active: configurationActive,
                }
            )

            configurations = configurations
                .map(configuration =>
                    configuration.id === updated.id
                        ? updated
                        : configuration
                )
                .sort((a, b) => a.name.localeCompare(b.name))

            cancelEdit()
        } catch (err) {
            saveError = err instanceof Error
                ? err.message
                : 'Unable to update configuration'
        } finally {
            saving = false
        }
    }



    onMount(() => {
        void loadConfigurations()
    })
</script>

<section class="mt-8 border-t border-slate-800 pt-6">

    <div class="mb-5 flex items-center justify-between gap-4">
        <h4 class="text-lg font-semibold text-white">
            Equipment Configurations
        </h4>

        {#if !showCreateForm && !editingConfigurationId}
            <button
                    type="button"
                    onclick={() => showCreateForm = true}
                    class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
            >
                + Add Configuration
            </button>
        {/if}
    </div>

    {#if showCreateForm}
        <form
                onsubmit={handleCreate}
                class="mb-6 rounded-lg border border-slate-700 bg-slate-800/50 p-5"
        >
            <div class="mb-5 flex items-center justify-between">
                <h5 class="font-medium text-white">
                    New Configuration
                </h5>

                <button
                        type="button"
                        onclick={() => {
                    showCreateForm = false
                    saveError = null
                }}
                        disabled={saving}
                        aria-label="Close configuration form"
                        class="text-slate-400 hover:text-white disabled:opacity-50"
                >
                    ✕
                </button>
            </div>

            <fieldset disabled={saving}>
                <label
                        for="configuration-name"
                        class="mb-2 block text-sm text-slate-300"
                >
                    Configuration Name
                </label>

                <input
                        id="configuration-name"
                        type="text"
                        bind:value={configurationName}
                        required
                        maxlength="255"
                        placeholder="e.g. Heavy Cargo"
                        class="mb-5 w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white"
                />

                <label class="flex items-center gap-3 text-sm text-slate-200">
                    <input
                            type="checkbox"
                            bind:checked={configurationActive}
                            class="h-4 w-4 accent-emerald-600"
                    />
                    Active configuration
                </label>
            </fieldset>

            {#if saveError}
                <p role="alert" class="mt-4 text-sm text-red-400">
                    {saveError}
                </p>
            {/if}

            <div class="mt-6 flex justify-end gap-3">
                <button
                        type="button"
                        onclick={() => showCreateForm = false}
                        disabled={saving}
                        class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 disabled:opacity-50"
                >
                    Cancel
                </button>

                <button
                        type="submit"
                        disabled={saving}
                        class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
                >
                    {saving ? 'Saving...' : 'Create Configuration'}
                </button>
            </div>
        </form>
    {/if}

    <p class="mb-5 text-sm text-slate-400">
        Operational arrangements associated with this equipment.
    </p>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading configurations...
        </p>
    {:else if error}
        <div>
            <p role="alert" class="text-sm text-red-400">{error}</p>
            <button
                    type="button"
                    onclick={loadConfigurations}
                    class="mt-2 text-sm text-emerald-400 hover:underline"
            >
                Retry
            </button>
        </div>
    {:else if configurations.length === 0}
        <p class="text-sm text-slate-400">
            No configurations registered for this equipment.
        </p>

    {:else}
        <div class="space-y-3">
            {#each configurations as configuration (configuration.id)}
                <div class="rounded-lg border border-slate-700 bg-slate-800/50 p-4">
                    <div class="flex items-center justify-between gap-4">
                        <p class="font-medium text-white">
                            {configuration.name}
                        </p>

                        <div class="flex flex-wrap items-center gap-3">
                        <span
                                class={configuration.active
                                ? 'rounded-full bg-emerald-900/50 px-3 py-1 text-xs text-emerald-300'
                                : 'rounded-full bg-slate-700 px-3 py-1 text-xs text-slate-300'}
                        >
                            {configuration.active ? 'Active' : 'Inactive'}
                        </span>

                            <button
                                    type="button"
                                    onclick={() => toggleConfigurationCapabilities(configuration.id)}
                                    class="rounded-full border border-slate-600 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700"
                            >
                                {expandedConfigurationId === configuration.id
                                    ? 'Hide Capabilities'
                                    : 'Capabilities'}
                            </button>

                            <button
                                    type="button"
                                    onclick={() => handleEdit(configuration)}
                                    class="text-sm text-emerald-400 hover:underline"
                            >
                                Edit
                            </button>
                        </div>
                    </div>

                    {#if expandedConfigurationId === configuration.id}
                        <div class="mt-4 border-t border-slate-700 pt-4">
                            <h5 class="mb-3 text-sm font-medium text-white">
                                Configuration Capabilities
                            </h5>

                            {#if capabilitiesLoading}
                                <p class="text-sm text-slate-400">
                                    Loading capabilities...
                                </p>
                            {:else}
                                <div class="flex flex-wrap gap-2">
                                    {#each EQUIPMENT_CAPABILITIES as capability}
                                        <button
                                                type="button"
                                                onclick={() => toggleCapability(configuration.id, capability)}
                                                disabled={capabilitiesSaving}
                                                aria-pressed={configurationCapabilities.includes(capability)}
                                                class={configurationCapabilities.includes(capability)
                                            ? 'rounded-full border border-emerald-600 bg-emerald-900/40 px-3 py-1.5 text-xs text-emerald-300 disabled:opacity-50'
                                            : 'rounded-full border border-slate-600 px-3 py-1.5 text-xs text-slate-400 hover:bg-slate-700 disabled:opacity-50'}
                                        >
                                            {capability.replaceAll('_', ' ')}
                                        </button>
                                    {/each}
                                </div>

                                <p class="mt-3 text-xs text-slate-400">
                                    Select a capability to assign it.
                                    Select it again to remove it.
                                </p>
                            {/if}

                            {#if capabilitiesError}
                                <p role="alert" class="mt-3 text-sm text-red-400">
                                    {capabilitiesError}
                                </p>
                            {/if}
                        </div>
                    {/if}
                </div>
            {/each}
        </div>
    {/if}


    {#if editingConfigurationId}
        <form
                onsubmit={handleUpdate}
                class="mb-6 rounded-lg border border-slate-700 bg-slate-800/50 p-5"
        >
            <div class="mb-5 flex items-center justify-between">
                <h5 class="font-medium text-white">
                    Edit Configuration
                </h5>

                <button
                        type="button"
                        onclick={cancelEdit}
                        disabled={saving}
                        aria-label="Close edit form"
                        class="text-slate-400 hover:text-white disabled:opacity-50"
                >
                    ✕
                </button>
            </div>

            <fieldset disabled={saving}>
                <label
                        for="edit-configuration-name"
                        class="mb-2 block text-sm text-slate-300"
                >
                    Configuration Name
                </label>

                <input
                        id="edit-configuration-name"
                        type="text"
                        bind:value={configurationName}
                        required
                        maxlength="255"
                        class="mb-5 w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white"
                />

                <label class="flex items-center gap-3 text-sm text-slate-200">
                    <input
                            type="checkbox"
                            bind:checked={configurationActive}
                            class="h-4 w-4 accent-emerald-600"
                    />
                    Active configuration
                </label>
            </fieldset>

            {#if saveError}
                <p role="alert" class="mt-4 text-sm text-red-400">
                    {saveError}
                </p>
            {/if}

            <div class="mt-6 flex justify-end gap-3">
                <button
                        type="button"
                        onclick={cancelEdit}
                        disabled={saving}
                        class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 disabled:opacity-50"
                >
                    Cancel
                </button>

                <button
                        type="submit"
                        disabled={saving}
                        class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
                >
                    {saving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </form>
    {/if}

</section>
