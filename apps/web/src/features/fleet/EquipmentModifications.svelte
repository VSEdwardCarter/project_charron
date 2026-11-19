
<script lang="ts">
    import { onMount } from 'svelte'
    import {
        getEquipmentModifications,
        recordEquipmentModification,
        type EquipmentModification,
        type EquipmentModificationType, removeEquipmentModification,
    } from '../../lib/api/equipmentModifications'

    interface Props {
        equipmentId: string
    }

    let { equipmentId }: Props = $props()

    const types: EquipmentModificationType[] = [
        'INSTALLATION',
        'REMOVAL',
        'REPLACEMENT',
        'ALTERATION',
    ]

    let modifications = $state<EquipmentModification[]>([])
    let loading = $state(true)
    let error = $state<string | null>(null)

    let showForm = $state(false)
    let saving = $state(false)
    let formError = $state<string | null>(null)

    let type = $state<EquipmentModificationType>('INSTALLATION')
    let description = $state('')
    let effectiveAt = $state('')

    let removingId = $state<string | null>(null)
    let removalDate = $state('')
    let removalError = $state<string | null>(null)
    let removalSaving = $state(false)

    function cancelRemoval(): void {
        removingId = null
        removalDate = ''
        removalError = null
    }

    async function handleRemoval(
        modification: EquipmentModification
    ): Promise<void> {
        if (!removalDate || removalSaving) return

        const effectiveAt = new Date(removalDate).toISOString()

        if (new Date(effectiveAt) < new Date(modification.effectiveAt)) {
            removalError =
                'Removal cannot precede the modification effective date.'
            return
        }

        removalSaving = true
        removalError = null

        try {
            await removeEquipmentModification(
                equipmentId,
                modification.id,
                effectiveAt
            )

            cancelRemoval()
            await loadModifications()
        } catch (err) {
            removalError = err instanceof Error
                ? err.message
                : 'Unable to remove modification'
        } finally {
            removalSaving = false
        }
    }


    onMount(() => {
        void loadModifications()
    })

    async function loadModifications(): Promise<void> {
        loading = true
        error = null

        try {
            modifications = await getEquipmentModifications(equipmentId)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve modifications'
        } finally {
            loading = false
        }
    }

    function formatDate(value: string): string {
        return new Date(value).toLocaleString()
    }

    async function handleSubmit(event: SubmitEvent): Promise<void> {
        event.preventDefault()

        if (!description.trim() || !effectiveAt || saving) return

        saving = true
        formError = null

        try {
            await recordEquipmentModification(equipmentId, {
                type,
                description: description.trim(),
                effectiveAt: new Date(effectiveAt).toISOString(),
            })

            showForm = false
            description = ''
            effectiveAt = ''

            await loadModifications()
        } catch (err) {
            formError = err instanceof Error
                ? err.message
                : 'Unable to record modification'
        } finally {
            saving = false
        }
    }
</script>

<div>
    <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
            <h3 class="text-lg font-semibold text-white">
                Equipment Modifications
            </h3>
            <p class="mt-1 text-sm text-slate-400">
                Record physical equipment changes and preserve their history.
            </p>
        </div>

        <button
                type="button"
                onclick={() => showForm = !showForm}
                class="rounded-full bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
        >
            {showForm ? 'Cancel' : '+ Record Modification'}
        </button>
    </div>

    {#if showForm}
        <form
                onsubmit={handleSubmit}
                class="mb-6 space-y-4 rounded-xl border border-slate-700 bg-slate-950 p-5"
        >
            <div>
                <label for="modification-type" class="mb-2 block text-sm text-slate-300">
                    Modification Type
                </label>

                <select
                        id="modification-type"
                        bind:value={type}
                        class="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white"
                >
                    {#each types as option}
                        <option value={option}>
                            {option.charAt(0) + option.slice(1).toLowerCase()}
                        </option>
                    {/each}
                </select>
            </div>

            <div>
                <label for="modification-description" class="mb-2 block text-sm text-slate-300">
                    Description
                </label>

                <textarea
                        id="modification-description"
                        bind:value={description}
                        required
                        rows="3"
                        placeholder="Describe the equipment modification"
                        class="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white"
                ></textarea>
            </div>

            <div>
                <label for="modification-effective-at" class="mb-2 block text-sm text-slate-300">
                    Effective Date and Time
                </label>

                <input
                        id="modification-effective-at"
                        type="datetime-local"
                        bind:value={effectiveAt}
                        required
                        class="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white"
                />

                <p class="mt-2 text-xs text-slate-400">
                    When the physical modification occurred.
                    CHARRON records the entry timestamp separately.
                </p>
            </div>

            {#if formError}
                <p class="text-sm text-red-400" role="alert">
                    {formError}
                </p>
            {/if}

            <div class="flex justify-end">
                <button
                        type="submit"
                        disabled={saving}
                        class="rounded-full bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
                >
                    {saving ? 'Saving...' : 'Save Modification'}
                </button>
            </div>
        </form>
    {/if}

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading modification history...
        </p>
    {:else if error}
        <p class="text-sm text-red-400">{error}</p>

        <button
                type="button"
                onclick={loadModifications}
                class="mt-3 text-sm text-emerald-400 hover:underline"
        >
            Retry
        </button>

    {:else if modifications.length === 0}
        <p class="rounded-lg border border-slate-800 bg-slate-950 p-5 text-sm text-slate-400">
            No equipment modifications recorded.
        </p>

    {:else}
        <div class="space-y-3">
            {#each modifications as modification (modification.id)}
                <article class="rounded-lg border border-slate-800 bg-slate-950 p-5">
                    <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
                        <span class="rounded-full bg-slate-800 px-3 py-1 text-xs font-medium text-slate-200">
                            {modification.type}
                        </span>

                        <span class="text-xs text-slate-400">
                            Effective: {formatDate(modification.effectiveAt)}
                        </span>
                    </div>

                    <p class="whitespace-pre-wrap text-sm text-white">
                        {modification.description}
                    </p>

                    <p class="mt-4 text-xs text-slate-500">
                        Recorded: {formatDate(modification.recordedAt)}
                    </p>


                    {#if modification.removedAt}
                        <p class="mt-3 text-xs text-amber-400">
                            Removed: {formatDate(modification.removedAt)}
                        </p>
                    {:else if removingId === modification.id}
                        <div class="mt-4 space-y-3 border-t border-slate-800 pt-4">
                            <label
                                    for="removal-effective-at"
                                    class="block text-sm text-slate-300"
                            >
                                When was this modification removed?
                            </label>

                            <input
                                    id="removal-effective-at"
                                    type="datetime-local"
                                    bind:value={removalDate}
                                    class="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white"
                            />

                            <p class="text-xs text-slate-400">
                                The original modification will remain in history.
                                CHARRON will record the removal timestamp separately.
                            </p>

                            {#if removalError}
                                <p class="text-sm text-red-400" role="alert">
                                    {removalError}
                                </p>
                            {/if}

                            <div class="flex flex-wrap gap-2">
                                <button
                                        type="button"
                                        onclick={() => handleRemoval(modification)}
                                        disabled={!removalDate || removalSaving}
                                        class="rounded-full bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-500 disabled:opacity-50"
                                >
                                    {removalSaving ? 'Saving...' : 'Confirm Removal'}
                                </button>

                                <button
                                        type="button"
                                        onclick={cancelRemoval}
                                        disabled={removalSaving}
                                        class="rounded-full border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800 disabled:opacity-50"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    {:else}
                        <button
                                type="button"
                                onclick={() => {
                                    removingId = modification.id
                                    removalDate = ''
                                    removalError = null
                                }}
                                class="mt-4 rounded-full border border-amber-700 px-4 py-2 text-sm text-amber-400 hover:bg-slate-800"
                        >
                            Mark as Removed
                        </button>
                    {/if}

                </article>
            {/each}
        </div>
    {/if}
</div>
