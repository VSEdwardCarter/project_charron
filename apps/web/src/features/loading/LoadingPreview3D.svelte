
<script lang="ts">
    import { onMount } from 'svelte'

    import CargoSpace3D from './CargoSpace3D.svelte'
    import LoadingCargoSelector from './LoadingCargoSelector.svelte'

    import { getCargo, type Cargo } from '../../lib/api/cargo'

    import {
        generateLoadingPreview,
        type LoadingPreviewResponse
    } from '../../lib/api/loadingPreview'

    import {
        buildLoadingPreviewCargo,
        type LoadingSelection
    } from '../../lib/models/loadingSelection'

    import {
        getMovementLoadingPlans
    } from '../../lib/api/loadingPlans'

    import {
        generateAndSaveArrangement,
        getCurrentArrangement,
        parseArrangementPreview,
        ArrangementApiError
    } from '../../lib/api/loadingPlanArrangements'

    interface Props {
        cargoSpaceId: string
        lengthMm: number
        widthMm: number
        heightMm: number
        movementId?: string | null
        equipmentId?: string | null
    }

    let {
        cargoSpaceId,
        lengthMm,
        widthMm,
        heightMm,
        movementId = null,
        equipmentId = null
    }: Props = $props()

    let catalog: Cargo[] = $state([])
    let selections: LoadingSelection[] = $state([])

    let catalogLoading = $state(false)
    let arrangementLoading = $state(false)
    let previewLoading = $state(false)

    let loadingPlanId: string | null = $state(null)

    let error: string | null = $state(null)
    let notice: string | null = $state(null)

    let preview: LoadingPreviewResponse | null = $state(null)

    let savedArrangement = $state(false)
    let arrangementStale = $state(false)

    const movementConnected = $derived(
        movementId !== null && equipmentId !== null
    )

    async function loadCatalog(): Promise<void> {
        catalogLoading = true
        error = null

        try {
            catalog = await getCargo()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to retrieve cargo catalog'
        } finally {
            catalogLoading = false
        }
    }

    function updateSelections(
        nextSelections: LoadingSelection[]
    ): void {
        selections = nextSelections
        preview = null
        error = null
        notice = null
        savedArrangement = false
    }

    async function resolveLoadingPlanId(): Promise<string> {
        if (!movementId || !equipmentId) {
            throw new Error(
                'Movement and equipment are required'
            )
        }

        const plans = await getMovementLoadingPlans(movementId)

        const plan = plans.find(
            item =>
                item.equipmentId === equipmentId &&
                item.cargoSpaceId === cargoSpaceId
        )

        if (!plan) {
            throw new Error(
                'No loading plan exists for this equipment ' +
                'and cargo space. Create a loading plan first.'
            )
        }

        loadingPlanId = plan.id

        return plan.id
    }

    async function loadSavedArrangement(): Promise<void> {
        if (!movementConnected) {
            return
        }

        arrangementLoading = true
        error = null
        notice = null
        preview = null
        savedArrangement = false
        arrangementStale = false

        try {
            const planId = await resolveLoadingPlanId()

            try {
                const arrangement =
                    await getCurrentArrangement(planId)

                const restored =
                    parseArrangementPreview(arrangement)

                if (restored.cargoSpaceId !== cargoSpaceId) {
                    throw new Error(
                        'Saved arrangement references another cargo space'
                    )
                }

                if (restored.status !== arrangement.status) {
                    throw new Error(
                        'Saved arrangement status is inconsistent'
                    )
                }

                preview = restored
                savedArrangement = true

                notice =
                    'Previously saved loading arrangement restored.'

            } catch (err) {
                if (
                    err instanceof ArrangementApiError &&
                    err.status === 404
                ) {
                    notice =
                        'No saved 3D arrangement exists yet. ' +
                        'Generate and Save to create one.'

                    return
                }

                if (
                    err instanceof ArrangementApiError &&
                    err.status === 409
                ) {
                    arrangementStale = true

                    notice =
                        'The saved arrangement is outdated. ' +
                        'Generate and Save to calculate a new one.'

                    return
                }

                throw err
            }

        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to restore saved arrangement'

        } finally {
            arrangementLoading = false
        }
    }

    async function generateMovementArrangement(): Promise<void> {
        if (previewLoading || arrangementLoading) {
            return
        }

        previewLoading = true
        error = null
        notice = null
        preview = null
        savedArrangement = false
        arrangementStale = false

        try {
            // The backend resolves the current planned cargo,
            // reads authoritative catalog measurements,
            // generates placements, and saves the result.

            const planId = await resolveLoadingPlanId()

            const arrangement =
                await generateAndSaveArrangement(planId)

            const generated =
                parseArrangementPreview(arrangement)

            if (generated.cargoSpaceId !== cargoSpaceId) {
                throw new Error(
                    'Generated arrangement references another cargo space'
                )
            }

            if (generated.status !== arrangement.status) {
                throw new Error(
                    'Generated arrangement status is inconsistent'
                )
            }

            preview = generated
            savedArrangement = true

            notice =
                'Loading arrangement generated and saved successfully.'

        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to generate and save loading arrangement'

        } finally {
            previewLoading = false
        }
    }

    async function generateIndependentPreview(): Promise<void> {
        if (
            previewLoading ||
            catalogLoading ||
            selections.length === 0
        ) {
            return
        }

        previewLoading = true
        error = null
        notice = null
        preview = null
        savedArrangement = false

        try {
            const cargo = buildLoadingPreviewCargo(
                catalog,
                selections
            )

            preview = await generateLoadingPreview({
                cargoSpaceId,
                cargo
            })

        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to generate loading preview'

        } finally {
            previewLoading = false
        }
    }

    async function generateLoad(): Promise<void> {
        if (movementConnected) {
            await generateMovementArrangement()
        } else {
            await generateIndependentPreview()
        }
    }

    onMount(() => {
        if (movementConnected) {
            void loadSavedArrangement()
        } else {
            void loadCatalog()
        }
    })
</script>

<div class="space-y-5">

    {#if movementConnected}
        <div
                class="rounded-lg border border-emerald-900/60
                   bg-emerald-950/20 p-4"
        >
            <h4 class="font-semibold text-emerald-400">
                Movement Loading Arrangement
            </h4>

            <p class="mt-2 text-sm text-slate-300">
                CHARRON generates loading arrangements using
                cargo assignments saved against this movement.
            </p>

            <p class="mt-2 text-xs text-slate-400">
                Generated arrangements are saved to PostgreSQL
                and restored when this workspace is reopened.
            </p>
        </div>

        {#if arrangementLoading}
            <p class="text-sm text-slate-400">
                Checking for a saved loading arrangement...
            </p>
        {/if}

    {:else}
        {#if catalogLoading}
            <p class="text-sm text-slate-400">
                Loading cargo catalog...
            </p>
        {:else}
            <LoadingCargoSelector
                    {catalog}
                    {selections}
                    onChange={updateSelections}
            />
        {/if}
    {/if}

    {#if notice}
        <div
                role="status"
                class="rounded-lg border border-slate-700
                   bg-slate-800/50 p-3 text-sm text-slate-200"
        >
            {notice}
        </div>
    {/if}

    {#if arrangementStale}
        <p class="text-sm text-amber-400">
            The previous arrangement is not displayed because
            its cargo inputs have changed.
        </p>
    {/if}

    {#if error}
        <p
                role="alert"
                class="rounded-lg border border-red-800
                   bg-red-950/40 p-3 text-sm text-red-300"
        >
            {error}
        </p>
    {/if}

    <div
            class="flex flex-wrap items-center justify-between gap-3
               border-t border-slate-800 pt-5"
    >
        <div>
            <h4 class="font-semibold text-white">
                3D Loading Arrangement
            </h4>

            <p class="mt-1 text-xs text-slate-400">
                Backend-generated cargo placements
            </p>

            {#if movementConnected}
                <p class="mt-1 text-xs text-emerald-400">
                    Source: Saved movement cargo
                </p>
            {:else}
                <p class="mt-1 text-xs text-slate-500">
                    Independent loading preview
                </p>
            {/if}
        </div>

        <button
                type="button"
                onclick={generateLoad}
                disabled={
                previewLoading ||
                arrangementLoading ||
                catalogLoading ||
                (!movementConnected && selections.length === 0)
            }
                class="rounded-lg bg-emerald-600 px-4 py-2
                   text-sm font-medium text-white
                   hover:bg-emerald-500
                   disabled:cursor-not-allowed
                   disabled:opacity-50"
        >
            {#if previewLoading}
                Generating...
            {:else if movementConnected}
                Generate &amp; Save
            {:else}
                Generate Load
            {/if}
        </button>
    </div>

    {#if preview}
        <div class="flex flex-wrap items-center gap-3 text-sm">

            <span
                    class={
                    preview.status === 'VALID'
                        ? 'font-semibold text-emerald-400'
                        : 'font-semibold text-amber-400'
                }
            >
                {preview.status}
            </span>

            <span class="text-slate-400">
                {preview.reason}
            </span>

            {#if preview.status === 'VALID'}
                <span class="text-slate-300">
                    {preview.placements.length}
                    cargo units positioned
                </span>
            {/if}

            {#if savedArrangement}
                <span class="text-xs text-emerald-400">
                    Saved in CHARRON
                </span>
            {/if}

        </div>
    {/if}

    {#key preview}
        <CargoSpace3D
                {lengthMm}
                {widthMm}
                {heightMm}
                placements={
                preview?.status === 'VALID'
                    ? preview.placements
                    : []
            }
        />
    {/key}

    <p class="text-xs text-slate-500">
        {#if movementConnected}
            Saved arrangements represent planned cargo placement,
            not confirmed physical loading.
        {:else}
            Independent previews are not saved.
        {/if}

        Geometric validation does not establish transport safety.
    </p>

</div>
