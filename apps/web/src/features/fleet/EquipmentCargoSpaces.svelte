
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        getEquipmentCargoSpaces,
        createEquipmentCargoSpace,
        cargoSpaceTypeLabels,
        type CargoSpaceType,
        type EquipmentCargoSpace,
        type CreateEquipmentCargoSpaceInput,
    } from '../../lib/api/equipmentCargoSpaces'

    interface Props {
        equipmentId: string
    }

    let { equipmentId }: Props = $props()

    const MM_PER_FOOT = 304.8

    let spaces = $state<EquipmentCargoSpace[]>([])
    let loading = $state(true)
    let saving = $state(false)
    let error = $state<string | null>(null)
    let formError = $state<string | null>(null)
    let showForm = $state(false)

    let name = $state('')
    let type = $state<CargoSpaceType>('ENCLOSED')
    let length = $state('')
    let width = $state('')
    let height = $state('')

    onMount(() => {
        void loadSpaces()
    })

    async function loadSpaces(): Promise<void> {
        loading = true
        error = null

        try {
            spaces = await getEquipmentCargoSpaces(equipmentId)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to load cargo spaces.'
        } finally {
            loading = false
        }
    }

    function feetToMm(value: string, field: string): number {
        const parsed = Number(value)

        if (!value.trim() ||
            !Number.isFinite(parsed) ||
            parsed <= 0) {
            throw new Error(
                `${field} must be greater than zero.`
            )
        }

        const millimeters = Math.round(parsed * MM_PER_FOOT)

        if (!Number.isSafeInteger(millimeters) ||
            millimeters <= 0) {
            throw new Error(
                `${field} is outside the supported range.`
            )
        }

        return millimeters
    }

    function formatFeet(mm: number): string {
        return (mm / MM_PER_FOOT).toFixed(2)
    }

    function formatVolume(space: EquipmentCargoSpace): string {
        if (space.usableHeightMm === null) {
            return 'Not defined'
        }

        const cubicMeters =
            (space.usableLengthMm / 1000) *
            (space.usableWidthMm / 1000) *
            (space.usableHeightMm / 1000)

        return `${cubicMeters.toFixed(2)} m³`
    }

    function resetForm(): void {
        name = ''
        type = 'ENCLOSED'
        length = ''
        width = ''
        height = ''
        formError = null
    }

    async function handleSubmit(event: SubmitEvent): Promise<void> {
        event.preventDefault()

        if (saving) return

        formError = null

        try {
            if (!name.trim()) {
                throw new Error('Cargo space name is required.')
            }

            const usableLengthMm =
                feetToMm(length, 'Usable length')

            const usableWidthMm =
                feetToMm(width, 'Usable width')

            const usableHeightMm = height.trim()
                ? feetToMm(height, 'Usable height')
                : null

            if (type === 'ENCLOSED' &&
                usableHeightMm === null) {
                throw new Error(
                    'Enclosed cargo spaces require usable height.'
                )
            }

            const data: CreateEquipmentCargoSpaceInput = {
                name: name.trim(),
                type,
                usableLengthMm,
                usableWidthMm,
                usableHeightMm,
            }

            saving = true

            await createEquipmentCargoSpace(
                equipmentId,
                data
            )

            await loadSpaces()

            resetForm()
            showForm = false

        } catch (err) {
            formError = err instanceof Error
                ? err.message
                : 'Unable to save cargo space.'
        } finally {
            saving = false
        }
    }
</script>

<div class="space-y-6">

    <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
            <h3 class="text-lg font-semibold text-white">
                Equipment Cargo Spaces
            </h3>

            <p class="mt-1 text-sm text-slate-400">
                Usable loading dimensions, separate from
                exterior equipment specifications.
            </p>
        </div>

        {#if !showForm}
            <button
                    type="button"
                    onclick={() => showForm = true}
                    class="rounded-lg bg-emerald-600 px-4 py-2 text-sm
                       font-medium text-white hover:bg-emerald-500"
            >
                + Add Cargo Space
            </button>
        {/if}
    </div>

    {#if loading}
        <p class="text-sm text-slate-400">
            Loading cargo spaces...
        </p>

    {:else if error}
        <div role="alert" class="rounded-lg border border-red-800 p-4">
            <p class="text-sm text-red-400">{error}</p>

            <button
                    type="button"
                    onclick={loadSpaces}
                    class="mt-3 text-sm text-emerald-400"
            >
                Retry
            </button>
        </div>

    {:else if spaces.length === 0}
        <div class="rounded-lg border border-dashed
                    border-slate-700 p-8 text-center">
            <p class="text-slate-300">
                No cargo spaces recorded for this equipment.
            </p>

            <p class="mt-2 text-sm text-slate-500">
                Add usable cargo dimensions to support
                future load planning.
            </p>
        </div>

    {:else}
        <div class="grid gap-4 lg:grid-cols-2">
            {#each spaces as space (space.id)}
                <div class="rounded-xl border border-slate-700
                            bg-slate-950 p-5">

                    <h4 class="font-semibold text-white">
                        {space.name}
                    </h4>

                    <p class="mt-1 text-sm text-emerald-400">
                        {cargoSpaceTypeLabels[space.type]}
                    </p>

                    <div class="mt-5 grid grid-cols-2 gap-4 text-sm">
                        <div>
                            <p class="text-slate-500">Usable length</p>
                            <p class="text-white">
                                {formatFeet(space.usableLengthMm)} ft
                            </p>
                        </div>

                        <div>
                            <p class="text-slate-500">Usable width</p>
                            <p class="text-white">
                                {formatFeet(space.usableWidthMm)} ft
                            </p>
                        </div>

                        <div>
                            <p class="text-slate-500">Usable height</p>
                            <p class="text-white">
                                {space.usableHeightMm === null
                                    ? 'Not defined'
                                    : `${formatFeet(space.usableHeightMm)} ft`}
                            </p>
                        </div>

                        <div>
                            <p class="text-slate-500">Nominal volume</p>
                            <p class="text-white">
                                {formatVolume(space)}
                            </p>
                        </div>
                    </div>
                </div>
            {/each}
        </div>
    {/if}

    {#if showForm}
        <form
                onsubmit={handleSubmit}
                class="rounded-xl border border-slate-700
                   bg-slate-950 p-6"
        >
            <h4 class="mb-5 font-semibold text-white">
                Register Cargo Space
            </h4>

            <fieldset disabled={saving} class="space-y-5">
                <div>
                    <label
                            for="cargo-space-name"
                            class="mb-2 block text-sm text-slate-300"
                    >
                        Name
                    </label>

                    <input
                            id="cargo-space-name"
                            type="text"
                            maxlength="150"
                            bind:value={name}
                            class="w-full rounded-lg border border-slate-700
                               bg-slate-900 px-3 py-2 text-white"
                            placeholder="Main Cargo Compartment"
                    />
                </div>

                <div>
                    <label
                            for="cargo-space-type"
                            class="mb-2 block text-sm text-slate-300"
                    >
                        Cargo Space Type
                    </label>

                    <select
                            id="cargo-space-type"
                            bind:value={type}
                            class="w-full rounded-lg border border-slate-700
                               bg-slate-900 px-3 py-2 text-white"
                    >
                        <option value="ENCLOSED">Enclosed</option>
                        <option value="OPEN_DECK">Open Deck</option>
                    </select>
                </div>

                <div class="grid gap-4 sm:grid-cols-3">
                    <div>
                        <label
                                for="cargo-length"
                                class="mb-2 block text-sm text-slate-300"
                        >
                            Usable Length (ft)
                        </label>

                        <input
                                id="cargo-length"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={length}
                                class="w-full rounded-lg border border-slate-700
                                   bg-slate-900 px-3 py-2 text-white"
                        />
                    </div>

                    <div>
                        <label
                                for="cargo-width"
                                class="mb-2 block text-sm text-slate-300"
                        >
                            Usable Width (ft)
                        </label>

                        <input
                                id="cargo-width"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={width}
                                class="w-full rounded-lg border border-slate-700
                                   bg-slate-900 px-3 py-2 text-white"
                        />
                    </div>

                    <div>
                        <label
                                for="cargo-height"
                                class="mb-2 block text-sm text-slate-300"
                        >
                            Usable Height (ft)
                        </label>

                        <input
                                id="cargo-height"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={height}
                                class="w-full rounded-lg border border-slate-700
                                   bg-slate-900 px-3 py-2 text-white"
                        />
                    </div>
                </div>

                <p class="text-xs text-slate-400">
                    Enclosed spaces require all three dimensions.
                    Open decks may leave height unspecified.
                    Measurements are stored in millimeters.
                </p>
            </fieldset>

            {#if formError}
                <p role="alert" class="mt-4 text-sm text-red-400">
                    {formError}
                </p>
            {/if}

            <div class="mt-6 flex justify-end gap-3">
                <button
                        type="button"
                        disabled={saving}
                        onclick={() => {
                        resetForm()
                        showForm = false
                    }}
                        class="rounded-lg border border-slate-700
                           px-4 py-2 text-sm text-slate-300
                           hover:bg-slate-800"
                >
                    Cancel
                </button>

                <button
                        type="submit"
                        disabled={saving}
                        class="rounded-lg bg-emerald-600 px-4 py-2
                           text-sm font-medium text-white
                           hover:bg-emerald-500 disabled:opacity-50"
                >
                    {saving ? 'Saving...' : 'Save Cargo Space'}
                </button>
            </div>
        </form>
    {/if}
</div>
