
<script lang="ts">
    import type {
        EquipmentSpecifications,
        EquipmentSpecificationsInput,
    } from '../../lib/api/equipmentSpecifications'

    interface Props {
        specifications: EquipmentSpecifications | null
        onCancel: () => void
        onSave: (data: EquipmentSpecificationsInput) => Promise<void>
    }

    let { specifications, onCancel, onSave }: Props = $props()

    const MM_PER_FOOT = 304.8
    const GRAMS_PER_POUND = 453.59237

    function toInput(value: number | null, factor: number): string {
        return value === null
            ? ''
            : String(Number((value / factor).toFixed(4)))
    }

    let length = $state(toInput(specifications?.lengthMm ?? null, MM_PER_FOOT))
    let width = $state(toInput(specifications?.widthMm ?? null, MM_PER_FOOT))
    let height = $state(toInput(specifications?.heightMm ?? null, MM_PER_FOOT))

    let hasWeightSpecifications = $state(
        specifications?.weightSpecificationsPresent ?? false
    )

    let emptyWeight = $state(
        toInput(specifications?.emptyWeightGrams ?? null, GRAMS_PER_POUND)
    )

    let grossWeightRating = $state(
        toInput(specifications?.grossWeightRatingGrams ?? null, GRAMS_PER_POUND)
    )

    let ratedPayloadCapacity = $state(
        toInput(specifications?.ratedPayloadCapacityGrams ?? null, GRAMS_PER_POUND)
    )

    let saving = $state(false)
    let error = $state<string | null>(null)


    function convert(
        value: string | number | undefined,
        factor: number
    ): number | null {
        if (value === undefined || value === null || value === '') {
            return null
        }

        const number = Number(value)

        if (!Number.isFinite(number) || number < 0) {
            throw new Error('Measurements must be nonnegative numbers.')
        }

        const converted = Math.round(number * factor)

        if (!Number.isSafeInteger(converted)) {
            throw new Error('Measurement exceeds the supported range.')
        }

        return converted
    }


    async function handleSubmit(event: SubmitEvent): Promise<void> {
        event.preventDefault()
        if (saving) return

        error = null

        try {
            const lengthMm = convert(length, MM_PER_FOOT)
            const widthMm = convert(width, MM_PER_FOOT)
            const heightMm = convert(height, MM_PER_FOOT)

            const dimensionCount = [
                lengthMm,
                widthMm,
                heightMm,
            ].filter(value => value !== null).length

            if (dimensionCount !== 0 && dimensionCount !== 3) {
                throw new Error(
                    'Provide all three dimensions or leave all three blank.'
                )
            }

            if (dimensionCount === 3 &&
                (lengthMm === 0 || widthMm === 0 || heightMm === 0)) {
                throw new Error('Dimensions must be greater than zero.')
            }

            const emptyWeightGrams = hasWeightSpecifications
                ? convert(emptyWeight, GRAMS_PER_POUND)
                : null

            const grossWeightRatingGrams = hasWeightSpecifications
                ? convert(grossWeightRating, GRAMS_PER_POUND)
                : null

            const ratedPayloadCapacityGrams = hasWeightSpecifications
                ? convert(ratedPayloadCapacity, GRAMS_PER_POUND)
                : null

            if (dimensionCount === 0 && !hasWeightSpecifications) {
                throw new Error(
                    'Provide dimensions or enable weight specifications.'
                )
            }

            if (grossWeightRatingGrams === 0) {
                throw new Error('Gross weight rating must be greater than zero.')
            }

            if (emptyWeightGrams !== null &&
                grossWeightRatingGrams !== null &&
                emptyWeightGrams > grossWeightRatingGrams) {
                throw new Error(
                    'Empty weight cannot exceed gross weight rating.'
                )
            }

            if (ratedPayloadCapacityGrams !== null &&
                emptyWeightGrams !== null &&
                grossWeightRatingGrams !== null &&
                ratedPayloadCapacityGrams >
                grossWeightRatingGrams - emptyWeightGrams) {
                throw new Error(
                    'Rated payload exceeds the available gross weight allowance.'
                )
            }

            const data: EquipmentSpecificationsInput = {
                lengthMm,
                widthMm,
                heightMm,
                weightSpecificationsPresent: hasWeightSpecifications,
                emptyWeightGrams,
                grossWeightRatingGrams,
                ratedPayloadCapacityGrams,
            }

            saving = true
            await onSave(data)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to save equipment specifications.'
        } finally {
            saving = false
        }
    }
</script>

<form
        onsubmit={handleSubmit}
        class="rounded-xl border border-slate-700 bg-slate-900 p-6"
>
    <div class="mb-6 flex items-center justify-between">
        <h3 class="text-lg font-semibold">
            {specifications ? 'Edit Specifications' : 'Add Specifications'}
        </h3>

        <button
                type="button"
                onclick={onCancel}
                disabled={saving}
                aria-label="Close specifications form"
                class="rounded-lg px-3 py-2 text-slate-400 hover:bg-slate-800 hover:text-white disabled:opacity-50"
        >
            ✕
        </button>
    </div>

    <fieldset disabled={saving}>
        <section class="mb-8">
            <h4 class="mb-4 font-medium text-white">
                Exterior Dimensions
            </h4>

            <p class="mb-4 text-sm text-slate-400">
                Enter all three measurements in feet, or leave them blank.
            </p>

            <div class="grid gap-4 sm:grid-cols-3">
                {#each [
                    { id: 'length', label: 'Length (ft)' },
                    { id: 'width', label: 'Width (ft)' },
                    { id: 'height', label: 'Height (ft)' },
                ] as field}
                    <div>
                        <label
                                for={field.id}
                                class="mb-2 block text-sm text-slate-300"
                        >
                            {field.label}
                        </label>

                        {#if field.id === 'length'}
                            <input id="length" type="number" min="0" step="any"
                                   bind:value={length}
                                   class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white" />
                        {:else if field.id === 'width'}
                            <input id="width" type="number" min="0" step="any"
                                   bind:value={width}
                                   class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white" />
                        {:else}
                            <input id="height" type="number" min="0" step="any"
                                   bind:value={height}
                                   class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white" />
                        {/if}
                    </div>
                {/each}
            </div>
        </section>

        <section>
            <label class="mb-5 flex items-center gap-3 text-sm text-slate-200">
                <input
                        type="checkbox"
                        bind:checked={hasWeightSpecifications}
                        class="h-4 w-4 accent-emerald-600"
                />
                Record weight specifications
            </label>

            {#if hasWeightSpecifications}
                <div class="grid gap-4 sm:grid-cols-3">
                    <div>
                        <label for="empty-weight" class="mb-2 block text-sm text-slate-300">
                            Empty Weight (lb)
                        </label>
                        <input
                                id="empty-weight"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={emptyWeight}
                                class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white"
                        />
                    </div>

                    <div>
                        <label for="gross-weight" class="mb-2 block text-sm text-slate-300">
                            Gross Weight Rating (lb)
                        </label>
                        <input
                                id="gross-weight"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={grossWeightRating}
                                class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white"
                        />
                    </div>

                    <div>
                        <label for="payload" class="mb-2 block text-sm text-slate-300">
                            Rated Payload (lb)
                        </label>
                        <input
                                id="payload"
                                type="number"
                                min="0"
                                step="any"
                                bind:value={ratedPayloadCapacity}
                                class="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white"
                        />
                    </div>
                </div>

                <p class="mt-3 text-xs text-slate-400">
                    Unknown weight measurements may be left blank.
                </p>
            {/if}
        </section>
    </fieldset>

    {#if error}
        <p role="alert" class="mt-6 text-sm text-red-400">
            {error}
        </p>
    {/if}

    <div class="mt-8 flex justify-end gap-3">
        <button
                type="button"
                onclick={onCancel}
                disabled={saving}
                class="rounded-lg border border-slate-700 px-5 py-2.5 text-sm text-slate-300 hover:bg-slate-800 disabled:opacity-50"
        >
            Cancel
        </button>

        <button
                type="submit"
                disabled={saving}
                class="rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
        >
            {saving ? 'Saving...' : 'Save Specifications'}
        </button>
    </div>
</form>
