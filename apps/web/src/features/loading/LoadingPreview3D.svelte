
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

    interface Props {
        cargoSpaceId: string
        lengthMm: number
        widthMm: number
        heightMm: number
    }

    let {
        cargoSpaceId,
        lengthMm,
        widthMm,
        heightMm
    }: Props = $props()

    let catalog: Cargo[] = $state([])
    let selections: LoadingSelection[] = $state([])

    let catalogLoading = $state(true)
    let previewLoading = $state(false)

    let error: string | null = $state(null)
    let preview: LoadingPreviewResponse | null = $state(null)

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

        // Existing placements are no longer current.
        preview = null
        error = null
    }

    async function createPreview(): Promise<void> {
        if (previewLoading || selections.length === 0) {
            return
        }

        previewLoading = true
        error = null
        preview = null

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

    onMount(() => {
        void loadCatalog()
    })
</script>

<div class="space-y-5">
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

    {#if error}
        <p
                role="alert"
                class="rounded-lg border border-red-800
             bg-red-950/40 p-3 text-sm text-red-300"
        >
            {error}
        </p>
    {/if}

    <div class="flex flex-wrap items-center justify-between gap-3
              border-t border-slate-800 pt-5">
        <div>
            <h4 class="font-semibold text-white">
                3D Loading Preview
            </h4>

            <p class="mt-1 text-xs text-slate-400">
                Backend-generated cargo placements
            </p>
        </div>

        <button
                type="button"
                onclick={createPreview}
                disabled={
        previewLoading ||
        catalogLoading ||
        selections.length === 0
      }
                class="rounded-lg bg-emerald-600 px-4 py-2
             text-sm font-medium text-white
             hover:bg-emerald-500
             disabled:cursor-not-allowed
             disabled:opacity-50"
        >
            {previewLoading ? 'Generating...' : 'Generate Load'}
        </button>
    </div>

    {#if preview}
        <div class="flex flex-wrap items-center gap-3 text-sm">
      <span
              class={preview.status === 'VALID'
          ? 'font-semibold text-emerald-400'
          : 'font-semibold text-amber-400'}
      >
        {preview.status}
      </span>

            <span class="text-slate-400">
        {preview.reason}
      </span>

            {#if preview.status === 'VALID'}
        <span class="text-slate-300">
          {preview.placements.length} cargo units positioned
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
        Preview only. No load assignment or inventory reservation is saved.
        Geometric validation does not establish transport safety.
    </p>
</div>
