
<script lang="ts">
    import {
        categoryLabels,
        type EquipmentCategory,
    } from '../../lib/models/equipment'

    interface Props {
        onCancel: () => void
        onSubmit: (data: EquipmentRegistrationData) => void
    }

    export interface EquipmentRegistrationData {
        name: string
        category: EquipmentCategory
        vin: string | null
    }

    let { onCancel, onSubmit }: Props = $props()

    let name = $state('')
    let category = $state<EquipmentCategory>('TRACTOR')
    let vin = $state('')

    const categories = Object.entries(categoryLabels) as [
        EquipmentCategory,
        string,
    ][]

    function handleSubmit(event: SubmitEvent) {
        event.preventDefault()

        const trimmedName = name.trim()
        if (!trimmedName) return

        onSubmit({
            name: trimmedName,
            category,
            vin: vin.trim() || null,
        })
    }
</script>

<form
        onsubmit={handleSubmit}
        class="rounded-xl border border-slate-800 bg-slate-900 p-6"
>
    <div class="mb-6 flex items-center justify-between">
        <h3 class="text-lg font-semibold">Register Equipment</h3>

        <button
                type="button"
                onclick={onCancel}
                class="text-sm text-slate-400 hover:text-white"
        >
            Cancel
        </button>
    </div>

    <div class="space-y-5">
        <div>
            <label for="equipment-name" class="mb-2 block text-sm text-slate-300">
                Equipment Name
            </label>

            <input
                    id="equipment-name"
                    type="text"
                    bind:value={name}
                    required
                    maxlength="255"
                    placeholder="e.g. Freightliner Cascadia"
                    class="w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white focus:border-emerald-500 focus:outline-none"
            />
        </div>

        <div>
            <label for="equipment-category" class="mb-2 block text-sm text-slate-300">
                Category
            </label>

            <select
                    id="equipment-category"
                    bind:value={category}
                    class="w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white focus:border-emerald-500 focus:outline-none"
            >
                {#each categories as [value, label]}
                    <option value={value}>{label}</option>
                {/each}
            </select>
        </div>

        <div>
            <label for="equipment-vin" class="mb-2 block text-sm text-slate-300">
                VIN <span class="text-slate-500">(Optional)</span>
            </label>

            <input
                    id="equipment-vin"
                    type="text"
                    bind:value={vin}
                    maxlength="17"
                    placeholder="Vehicle Identification Number"
                    class="w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white focus:border-emerald-500 focus:outline-none"
            />
        </div>
    </div>

    <div class="mt-8 flex justify-end gap-3">
        <button
                type="button"
                onclick={onCancel}
                class="rounded-lg border border-slate-700 px-5 py-2.5 text-sm text-slate-300 hover:bg-slate-800"
        >
            Cancel
        </button>

        <button
                type="submit"
                class="rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-500"
        >
            Register Equipment
        </button>
    </div>
</form>
