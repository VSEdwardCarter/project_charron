
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        addLoadingPlanCargo,
        getLoadingPlanCargo,
        updateLoadingPlanCargoQuantity,
        removeLoadingPlanCargo,
        type LoadingPlanCargo
    } from '../../lib/api/loadingPlanCargo'

    interface CatalogCargo {
        id: string
        name: string
        reference?: string | null
        weightGrams?: number | null
        lengthMm?: number | null
        widthMm?: number | null
        heightMm?: number | null
    }

    interface Props {
        planId: string
    }

    let { planId }: Props = $props()

    let catalog: CatalogCargo[] = $state([])
    let cargoLines: LoadingPlanCargo[] = $state([])

    let selectedCargoId = $state('')
    let quantity = $state(1)

    let loading = $state(true)
    let working = $state(false)
    let error: string | null = $state(null)
    let message: string | null = $state(null)

    let editedQuantities: Record<string, number> = $state({})

    const assignedCargoIds = $derived(
        new Set(cargoLines.map((line) => line.cargoId))
    )

    const availableCargo = $derived(
        catalog.filter((item) => !assignedCargoIds.has(item.id))
    )

    function cargoName(cargoId: string): string {
        return catalog.find((item) => item.id === cargoId)?.name
            ?? cargoId
    }

    async function loadData() {
        loading = true
        error = null

        try {
            const [catalogResponse, lines] = await Promise.all([
                fetch('/api/cargo'),
                getLoadingPlanCargo(planId)
            ])

            if (!catalogResponse.ok) {
                throw new Error(
                    `Unable to retrieve cargo catalog: HTTP ${catalogResponse.status}`
                )
            }

            const catalogItems = await catalogResponse.json()

            if (!Array.isArray(catalogItems)) {
                throw new Error('Unexpected cargo catalog response')
            }

            catalog = catalogItems as CatalogCargo[]
            cargoLines = lines

            editedQuantities = Object.fromEntries(
                lines.map((line) => [line.id, line.quantity])
            )
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to load cargo'
        } finally {
            loading = false
        }
    }

    async function addCargo() {
        if (!selectedCargoId || !Number.isInteger(quantity) || quantity <= 0) {
            error = 'Select cargo and enter a positive whole-number quantity.'
            return
        }

        working = true
        error = null
        message = null

        try {
            const added = await addLoadingPlanCargo(planId, {
                cargoId: selectedCargoId,
                quantity
            })

            cargoLines = [...cargoLines, added]
            editedQuantities[added.id] = added.quantity

            selectedCargoId = ''
            quantity = 1
            message = 'Cargo added to the loading plan.'
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to add cargo'
        } finally {
            working = false
        }
    }

    async function updateQuantity(line: LoadingPlanCargo) {
        const newQuantity = editedQuantities[line.id]

        if (!Number.isInteger(newQuantity) || newQuantity <= 0) {
            error = 'Quantity must be a positive whole number.'
            return
        }

        working = true
        error = null
        message = null

        try {
            const updated = await updateLoadingPlanCargoQuantity(
                planId,
                line.id,
                newQuantity
            )

            cargoLines = cargoLines.map((item) =>
                item.id === line.id ? updated : item
            )

            message = 'Cargo quantity updated.'
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to update cargo quantity'
        } finally {
            working = false
        }
    }

    async function removeCargo(line: LoadingPlanCargo) {
        working = true
        error = null
        message = null

        try {
            await removeLoadingPlanCargo(planId, line.id)

            cargoLines = cargoLines.filter(
                (item) => item.id !== line.id
            )

            delete editedQuantities[line.id]

            message = 'Cargo removed from the loading plan.'
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to remove cargo'
        } finally {
            working = false
        }
    }

    onMount(() => {
        void loadData()
    })
</script>

<section class="space-y-4 rounded-lg border p-4">
    <div>
        <h3 class="text-lg font-semibold">Planned Cargo</h3>
        <p class="text-sm opacity-70">
            Assign cargo from the catalog to this loading plan.
            These assignments do not confirm physical loading.
        </p>
    </div>

    {#if loading}
        <p class="text-sm">Loading cargo...</p>
    {:else}
        <div class="flex flex-wrap items-end gap-3">
            <label class="flex min-w-64 flex-1 flex-col gap-1 text-sm">
                Cargo

                <select
                        bind:value={selectedCargoId}
                        disabled={working}
                        class="rounded-md border border-slate-600 bg-slate-900 p-2 text-white"
                        style="color-scheme: dark;"
                >
                    <option value="" class="bg-slate-900 text-white">
                        Select cargo
                    </option>

                    {#each availableCargo as item (item.id)}
                        <option
                                value={item.id}
                                class="bg-slate-900 text-white"
                        >
                            {item.name}{item.reference ? ` (${item.reference})` : ''}
                        </option>
                    {/each}
                </select>

            </label>

            <label class="flex w-28 flex-col gap-1 text-sm">
                Quantity
                <input
                        type="number"
                        min="1"
                        step="1"
                        bind:value={quantity}
                        disabled={working}
                        class="rounded-md border p-2"
                />
            </label>

            <button
                    type="button"
                    onclick={addCargo}
                    disabled={working || !selectedCargoId}
                    class="rounded-md border px-4 py-2 font-medium disabled:opacity-50"
            >
                {working ? 'Working...' : 'Add Cargo'}
            </button>
        </div>

        {#if cargoLines.length === 0}
            <p class="text-sm opacity-70">
                No cargo assigned to this loading plan.
            </p>
        {:else}
            <div class="overflow-x-auto">
                <table class="w-full text-left text-sm">
                    <thead>
                    <tr class="border-b">
                        <th class="p-2">Cargo</th>
                        <th class="p-2">Quantity</th>
                        <th class="p-2">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {#each cargoLines as line (line.id)}
                        <tr class="border-b">
                            <td class="p-2">{cargoName(line.cargoId)}</td>
                            <td class="p-2">
                                <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        bind:value={editedQuantities[line.id]}
                                        disabled={working}
                                        class="w-24 rounded-md border p-2"
                                        aria-label={`Quantity for ${cargoName(line.cargoId)}`}
                                />
                            </td>
                            <td class="p-2">
                                <div class="flex flex-wrap gap-2">
                                    <button
                                            type="button"
                                            onclick={() => updateQuantity(line)}
                                            disabled={working || editedQuantities[line.id] === line.quantity}
                                            class="rounded-md border px-3 py-1 disabled:opacity-50"
                                    >
                                        Update
                                    </button>

                                    <button
                                            type="button"
                                            onclick={() => removeCargo(line)}
                                            disabled={working}
                                            class="rounded-md border px-3 py-1 disabled:opacity-50"
                                    >
                                        Remove
                                    </button>
                                </div>
                            </td>
                        </tr>
                    {/each}
                    </tbody>
                </table>
            </div>
        {/if}
    {/if}

    {#if message}
        <p class="text-sm text-green-600" role="status">
            {message}
        </p>
    {/if}

    {#if error}
        <p class="text-sm text-red-600" role="alert">
            {error}
        </p>
        <button
                type="button"
                onclick={loadData}
                disabled={working || loading}
                class="rounded-md border px-3 py-1 text-sm"
        >
            Retry
        </button>
    {/if}
</section>
