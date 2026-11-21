
<script lang="ts">
    import CargoSpace3D from './CargoSpace3D.svelte'

    import {
        generateLoadingPreview,
        type LoadingPreviewResponse,
        type LoadingPreviewRequest
    } from '../../lib/api/loadingPreview'

    type Props = {
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

    let preview = $state<LoadingPreviewResponse | null>(null)
    let loading = $state(false)
    let error = $state<string | null>(null)

    async function createPreview() {
        loading = true
        error = null
        preview = null

        // Temporary synthetic development cargo.
        // Positions are NOT supplied by the frontend.
        const request: LoadingPreviewRequest = {
            cargoSpaceId,
            cargo: [
                {
                    cargoId: '11111111-1111-4111-8111-111111111111',
                    name: 'Demo Generator',
                    quantity: 3,
                    lengthMm: 1200,
                    widthMm: 800,
                    heightMm: 1000,
                    weightGrams: 150000
                },
                {
                    cargoId: '22222222-2222-4222-8222-222222222222',
                    name: 'Demo Supply Crate',
                    quantity: 4,
                    lengthMm: 600,
                    widthMm: 600,
                    heightMm: 600,
                    weightGrams: 25000
                }
            ]
        }

        try {
            preview = await generateLoadingPreview(request)
        } catch (e) {
            error = e instanceof Error
                ? e.message
                : 'Unable to generate loading preview'
        } finally {
            loading = false
        }
    }
</script>

<div class="space-y-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
            <h4 class="font-semibold text-white">
                3D Loading Preview
            </h4>

            <p class="mt-1 text-xs text-slate-400">
                Demonstration cargo · Backend-generated positions
            </p>
        </div>

        <button
                type="button"
                onclick={createPreview}
                disabled={loading}
                class="rounded-lg bg-emerald-600 px-4 py-2
             text-sm font-medium text-white
             hover:bg-emerald-500
             disabled:cursor-not-allowed
             disabled:opacity-50"
        >
            {loading ? 'Generating...' : 'Generate Demo Load'}
        </button>
    </div>

    {#if error}
        <p class="rounded-lg border border-red-800
              bg-red-950/40 p-3 text-sm text-red-300">
            {error}
        </p>
    {/if}

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
                placements={preview?.status === 'VALID'
        ? preview.placements
        : []}
        />
    {/key}

    <p class="text-xs text-slate-500">
        Preview only. No loading plan is saved.
        Geometric validation does not establish transport safety.
    </p>
</div>
