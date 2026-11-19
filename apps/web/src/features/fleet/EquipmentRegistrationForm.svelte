
<script lang="ts">
    import {
        categoryLabels,
        type EquipmentCategory,
    } from '../../lib/models/equipment'
    import {
        decodeVehicleVin,
        type DecodedVehicle,
    } from '../../lib/api/vehicleVin'

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

    let decodedVehicle = $state<DecodedVehicle | null>(null)
    let decoding = $state(false)
    let decodeError = $state<string | null>(null)

    const categories = Object.entries(categoryLabels) as [
        EquipmentCategory,
        string,
    ][]

    async function handleDecode(): Promise<void> {
        const normalizedVin = vin.trim().toUpperCase()

        decodedVehicle = null
        decodeError = null

        if (!/^[A-HJ-NPR-Z0-9]{17}$/.test(normalizedVin)) {
            decodeError = 'Enter a valid 17-character VIN.'
            return
        }

        vin = normalizedVin
        decoding = true

        try {
            const result = await decodeVehicleVin(normalizedVin)

            if (vin.trim().toUpperCase() !== normalizedVin) return

            decodedVehicle = result

            const suggestedName = [
                result.modelYear,
                result.make,
                result.model,
            ].filter(Boolean).join(' ')

            if (!name.trim() && suggestedName) {
                name = suggestedName
            }
        } catch (error) {
            if (vin.trim().toUpperCase() !== normalizedVin) return

            decodeError = error instanceof Error
                ? error.message
                : 'Unable to decode VIN'
        } finally {
            decoding = false
        }
    }

    function handleSubmit(event: SubmitEvent) {
        event.preventDefault()

        const trimmedName = name.trim()
        if (!trimmedName) return

        onSubmit({
            name: trimmedName,
            category,
            vin: vin.trim().toUpperCase() || null,
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

            <div class="flex gap-3">
                <input
                        id="equipment-vin"
                        type="text"
                        bind:value={vin}
                        oninput={() => {
                        decodedVehicle = null
                        decodeError = null
                    }}
                        maxlength="17"
                        placeholder="Vehicle Identification Number"
                        class="min-w-0 flex-1 rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white focus:border-emerald-500 focus:outline-none"
                />

                <button
                        type="button"
                        onclick={handleDecode}
                        disabled={decoding || vin.trim().length !== 17}
                        class="rounded-lg bg-slate-700 px-4 py-3 text-sm font-medium text-white hover:bg-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {decoding ? 'Decoding...' : 'Decode VIN'}
                </button>
            </div>

            {#if decodeError}
                <p class="mt-2 text-sm text-red-400" role="alert">
                    {decodeError}
                </p>
            {/if}
        </div>

        {#if decodedVehicle}
            <section class="rounded-lg border border-emerald-800 bg-slate-950 p-5">
                <h4 class="mb-3 font-semibold text-emerald-400">
                    Decoded Vehicle Information
                </h4>

                <dl class="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                    {#each [
                        ['Manufacturer', decodedVehicle.manufacturer],
                        ['Make', decodedVehicle.make],
                        ['Model', decodedVehicle.model],
                        ['Year', decodedVehicle.modelYear],
                        ['Trim', decodedVehicle.trim],
                        ['Vehicle Type', decodedVehicle.vehicleType],
                        ['Body Class', decodedVehicle.bodyClass],
                        ['Engine Manufacturer', decodedVehicle.engineManufacturer],
                        ['Engine Model', decodedVehicle.engineModel],
                        ['Engine Configuration', decodedVehicle.engineConfiguration],
                        ['Cylinders', decodedVehicle.engineCylinders],
                        ['Displacement (L)', decodedVehicle.displacementLiters],
                        ['Horsepower', decodedVehicle.engineHorsepower],
                        ['Fuel Type', decodedVehicle.fuelType],
                        ['Transmission', decodedVehicle.transmission],
                        ['Drive Type', decodedVehicle.driveType],
                        ['GVWR Class', decodedVehicle.gvwrClass],
                    ] as [string, string | number | null][] as item}
                        <div>
                            <dt class="text-slate-500">{item[0]}</dt>
                            <dd class="mt-1 text-slate-200">
                                {item[1] ?? 'Unknown'}
                            </dd>
                        </div>
                    {/each}
                </dl>

                <p class="mt-4 text-xs text-slate-400">
                    VIN data describes manufacturer-reported vehicle
                    characteristics. Verify current equipment specifications.
                    Only the equipment name, category, and VIN will be saved
                    during registration.
                </p>
            </section>
        {/if}
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
