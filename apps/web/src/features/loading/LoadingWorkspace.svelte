
<script lang="ts">
    import LoadingPreview3D from './LoadingPreview3D.svelte'

    interface Props {
        equipmentName: string
        cargoSpaceId: string
        cargoSpaceName: string
        lengthMm: number
        widthMm: number
        heightMm: number
        onBack: () => void
    }

    let {
        equipmentName,
        cargoSpaceId,
        cargoSpaceName,
        lengthMm,
        widthMm,
        heightMm,
        onBack
    }: Props = $props()
</script>

<div class="space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
            <h3 class="text-xl font-semibold text-white">
                Loading Workspace
            </h3>

            <p class="mt-1 text-sm text-slate-400">
                Plan cargo placement and inspect equipment loading arrangements.
            </p>
        </div>

        <button
                type="button"
                onclick={onBack}
                class="rounded-lg border border-slate-700
             px-4 py-2 text-sm text-slate-300
             hover:bg-slate-800"
        >
            Back
        </button>
    </div>

    <div class="grid gap-4 lg:grid-cols-3">
        <div class="rounded-xl border border-slate-800
                bg-slate-900 p-5 lg:col-span-1">
            <h4 class="font-semibold text-white">
                Equipment Context
            </h4>

            <dl class="mt-4 space-y-3 text-sm">
                <div>
                    <dt class="text-slate-400">Equipment</dt>
                    <dd class="mt-1 text-white">{equipmentName}</dd>
                </div>

                <div>
                    <dt class="text-slate-400">Cargo Space</dt>
                    <dd class="mt-1 text-white">{cargoSpaceName}</dd>
                </div>

                <div>
                    <dt class="text-slate-400">Usable Dimensions</dt>
                    <dd class="mt-1 text-white">
                        {lengthMm} × {widthMm} × {heightMm} mm
                    </dd>
                </div>
            </dl>
        </div>

        <div class="min-w-0 rounded-xl border border-slate-800
                bg-slate-900 p-5 lg:col-span-2">
            <LoadingPreview3D
                    {cargoSpaceId}
                    {lengthMm}
                    {widthMm}
                    {heightMm}
            />
        </div>
    </div>
</div>
