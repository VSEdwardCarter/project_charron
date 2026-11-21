
<script lang="ts">
    import type {
        CargoRegistrationData
    } from '../../lib/api/cargo'

    interface Props {
        onSubmit: (data: CargoRegistrationData) => Promise<void>
        onCancel: () => void
    }

    let { onSubmit, onCancel }: Props = $props()

    let name = $state('')
    let reference = $state('')
    let description = $state('')

    let weightKg = $state('')
    let lengthMm = $state('')
    let widthMm = $state('')
    let heightMm = $state('')

    let submitting = $state(false)
    let error = $state<string | null>(null)

    function optionalText(value: string): string | null {
        const normalized = value.trim()
        return normalized.length > 0 ? normalized : null
    }

    function optionalDimension(
        value: string,
        label: string
    ): number | null {
        const trimmed = value.trim()

        if (!trimmed) return null

        if (!/^\d+$/.test(trimmed)) {
            throw new Error(`${label} must be a whole number`)
        }

        const parsed = Number(trimmed)

        if (!Number.isSafeInteger(parsed) || parsed <= 0) {
            throw new Error(`${label} must be a positive integer`)
        }

        return parsed
    }

    function optionalWeightGrams(value: string): number | null {
        const trimmed = value.trim()

        if (!trimmed) return null

        if (!/^\d+(?:\.\d{1,3})?$/.test(trimmed)) {
            throw new Error(
                'Weight must be a positive number with at most 3 decimal places'
            )
        }

        const grams = Number(trimmed) * 1000
        const rounded = Math.round(grams)

        if (!Number.isSafeInteger(rounded) || rounded <= 0) {
            throw new Error('Weight must be greater than zero')
        }

        return rounded
    }

    async function handleSubmit(event: SubmitEvent) {
        event.preventDefault()
        if (submitting) return

        error = null

        try {
            if (!name.trim()) {
                throw new Error('Cargo name is required')
            }

            const length = optionalDimension(lengthMm, 'Length')
            const width = optionalDimension(widthMm, 'Width')
            const height = optionalDimension(heightMm, 'Height')

            const dimensions = [length, width, height]
            const provided = dimensions.filter(
                value => value !== null
            ).length

            if (provided !== 0 && provided !== 3) {
                throw new Error(
                    'Length, width, and height must be provided together'
                )
            }

            const data: CargoRegistrationData = {
                name: name.trim(),
                reference: optionalText(reference),
                description: optionalText(description),
                weightGrams: optionalWeightGrams(weightKg),
                lengthMm: length,
                widthMm: width,
                heightMm: height
            }

            submitting = true
            await onSubmit(data)
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to register cargo'
        } finally {
            submitting = false
        }
    }
</script>

<form
        onsubmit={handleSubmit}
        class="space-y-6 rounded-xl border border-slate-700
         bg-slate-900 p-6"
>
    <div class="flex items-center justify-between">
        <h4 class="text-lg font-semibold text-white">
            Register Cargo
        </h4>

        <button
                type="button"
                onclick={onCancel}
                disabled={submitting}
                class="text-sm text-slate-400 hover:text-white"
        >
            Cancel
        </button>
    </div>

    {#if error}
        <div
                role="alert"
                class="rounded-lg border border-red-800
             bg-red-950/40 p-3 text-sm text-red-300"
        >
            {error}
        </div>
    {/if}

    <div class="grid gap-4 md:grid-cols-2">
        <label class="block">
      <span class="text-sm text-slate-300">
        Cargo Name *
      </span>

            <input
                    type="text"
                    bind:value={name}
                    maxlength="255"
                    required
                    placeholder="Portable Generator"
                    class="mt-2 w-full rounded-lg border border-slate-700
               bg-slate-950 px-3 py-2 text-sm text-white"
            />
        </label>

        <label class="block">
      <span class="text-sm text-slate-300">
        Reference / SKU
      </span>

            <input
                    type="text"
                    bind:value={reference}
                    maxlength="255"
                    placeholder="GEN-001"
                    class="mt-2 w-full rounded-lg border border-slate-700
               bg-slate-950 px-3 py-2 text-sm text-white"
            />
        </label>
    </div>

    <label class="block">
    <span class="text-sm text-slate-300">
      Description
    </span>

        <textarea
                bind:value={description}
                rows="3"
                placeholder="Additional cargo information"
                class="mt-2 w-full rounded-lg border border-slate-700
             bg-slate-950 px-3 py-2 text-sm text-white"
        ></textarea>
    </label>

    <div class="border-t border-slate-800 pt-5">
        <h5 class="font-medium text-white">
            Physical Measurements
        </h5>

        <p class="mt-1 text-xs text-slate-400">
            Measurements are optional. If dimensions are supplied,
            all three must be provided.
        </p>

        <div class="mt-4 grid gap-4 md:grid-cols-4">
            <label class="block">
        <span class="text-sm text-slate-300">
          Weight (kg)
        </span>

                <input
                        type="text"
                        inputmode="decimal"
                        bind:value={weightKg}
                        placeholder="150"
                        class="mt-2 w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2 text-sm text-white"
                />
            </label>

            <label class="block">
        <span class="text-sm text-slate-300">
          Length (mm)
        </span>

                <input
                        type="text"
                        inputmode="numeric"
                        bind:value={lengthMm}
                        placeholder="1200"
                        class="mt-2 w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2 text-sm text-white"
                />
            </label>

            <label class="block">
        <span class="text-sm text-slate-300">
          Width (mm)
        </span>

                <input
                        type="text"
                        inputmode="numeric"
                        bind:value={widthMm}
                        placeholder="800"
                        class="mt-2 w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2 text-sm text-white"
                />
            </label>

            <label class="block">
        <span class="text-sm text-slate-300">
          Height (mm)
        </span>

                <input
                        type="text"
                        inputmode="numeric"
                        bind:value={heightMm}
                        placeholder="1000"
                        class="mt-2 w-full rounded-lg border border-slate-700
                 bg-slate-950 px-3 py-2 text-sm text-white"
                />
            </label>
        </div>
    </div>

    <div class="flex justify-end gap-3 border-t border-slate-800 pt-5">
        <button
                type="button"
                onclick={onCancel}
                disabled={submitting}
                class="rounded-lg bg-slate-800 px-4 py-2
             text-sm text-white hover:bg-slate-700"
        >
            Cancel
        </button>

        <button
                type="submit"
                disabled={submitting}
                class="rounded-lg bg-emerald-600 px-4 py-2
             text-sm font-medium text-white
             hover:bg-emerald-500 disabled:opacity-50"
        >
            {submitting ? 'Registering...' : 'Register Cargo'}
        </button>
    </div>
</form>
