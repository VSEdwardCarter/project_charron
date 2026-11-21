
<script lang="ts">
    import type { Cargo } from '../../lib/api/cargo'
    import {
        isCargoLoadable,
        type LoadingSelection
    } from '../../lib/models/loadingSelection'

    interface Props {
        catalog: Cargo[]
        selections: LoadingSelection[]
        onChange: (selections: LoadingSelection[]) => void
    }

    let {
        catalog,
        selections,
        onChange
    }: Props = $props()

    let selectedCargoId = $state('')
    let quantity = $state(1)

    const availableCargo = $derived(
        catalog.filter(cargo =>
            isCargoLoadable(cargo) &&
            !selections.some(selection =>
                selection.cargoId === cargo.id
            )
        )
    )

    function addCargo(): void {
        if (!selectedCargoId) return

        if (!Number.isSafeInteger(quantity) || quantity <= 0) {
            return
        }

        const cargo = availableCargo.find(
            item => item.id === selectedCargoId
        )

        if (!cargo) return

        onChange([
            ...selections,
            {
                cargoId: cargo.id,
                quantity
            }
        ])

        selectedCargoId = ''
        quantity = 1
    }

    function updateQuantity(
        cargoId: string,
        nextQuantity: number
    ): void {
        if (!Number.isSafeInteger(nextQuantity) || nextQuantity <= 0) {
            return
        }

        onChange(
            selections.map(selection =>
                selection.cargoId === cargoId
                    ? { ...selection, quantity: nextQuantity }
                    : selection
            )
        )
    }

    function removeCargo(cargoId: string): void {
        onChange(
            selections.filter(selection =>
                selection.cargoId !== cargoId
            )
        )
    }

    function cargoName(cargoId: string): string {
        return catalog.find(item => item.id === cargoId)?.name
            ?? 'Unknown Cargo'
    }
</script>

<div class="space-y-4">
    <div>
        <h4 class="font-semibold text-white">
            Loading Cargo
        </h4>

        <p class="mt-1 text-xs text-slate-400">
            Select registered cargo and specify the quantity to load.
        </p>
    </div>

    <div class="grid gap-3 md:grid-cols-[1fr_110px_auto]">
        <select
                bind:value={selectedCargoId}
                class="rounded-lg border border-slate-700
             bg-slate-950 px-3 py-2 text-sm text-white"
        >
            <option value="">Select cargo...</option>

            {#each availableCargo as item (item.id)}
                <option value={item.id}>
                    {item.name} ({item.reference ?? 'No reference'})
                </option>
            {/each}
        </select>

        <input
                type="number"
                min="1"
                step="1"
                bind:value={quantity}
                class="rounded-lg border border-slate-700
             bg-slate-950 px-3 py-2 text-sm text-white"
        />

        <button
                type="button"
                onclick={addCargo}
                disabled={!selectedCargoId}
                class="rounded-lg bg-emerald-600 px-4 py-2
             text-sm font-medium text-white
             hover:bg-emerald-500 disabled:opacity-50"
        >
            Add
        </button>
    </div>

    {#if selections.length === 0}
        <div class="rounded-lg border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
            No cargo selected for this load.
        </div>
    {:else}
        <div class="overflow-x-auto rounded-lg border border-slate-800">
            <table class="w-full text-left text-sm">
                <thead class="bg-slate-800/50 text-xs uppercase text-slate-400">
                <tr>
                    <th class="px-4 py-3 font-medium">Cargo</th>
                    <th class="px-4 py-3 font-medium">Quantity</th>
                    <th class="px-4 py-3 font-medium">Action</th>
                </tr>
                </thead>

                <tbody class="divide-y divide-slate-800">
                {#each selections as selection (selection.cargoId)}
                    <tr>
                        <td class="px-4 py-3 text-white">
                            {cargoName(selection.cargoId)}
                        </td>

                        <td class="px-4 py-3">
                            <input
                                    type="number"
                                    min="1"
                                    step="1"
                                    value={selection.quantity}
                                    onchange={event =>
                    updateQuantity(
                      selection.cargoId,
                      Number(event.currentTarget.value)
                    )
                  }
                                    class="w-24 rounded-lg border border-slate-700
                         bg-slate-950 px-3 py-2 text-white"
                            />
                        </td>

                        <td class="px-4 py-3">
                            <button
                                    type="button"
                                    onclick={() => removeCargo(selection.cargoId)}
                                    class="text-red-400 hover:text-red-300"
                            >
                                Remove
                            </button>
                        </td>
                    </tr>
                {/each}
                </tbody>
            </table>
        </div>
    {/if}
</div>
