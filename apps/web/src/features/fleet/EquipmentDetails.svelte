
<script lang="ts">
    import {
        categoryLabels,
        type Equipment,
    } from '../../lib/models/equipment'

    interface Props {
        equipment: Equipment
        onBack: () => void
    }

    let { equipment, onBack }: Props = $props()
</script>

<div class="rounded-xl border border-slate-800 bg-slate-900 p-6">
    <button
            type="button"
            onclick={onBack}
            class="mb-6 text-sm text-slate-400 hover:text-white"
    >
        ← Back to Fleet
    </button>

    <div class="mb-6">
        <h3 class="text-xl font-semibold text-white">
            {equipment.name}
        </h3>

        <p class="mt-1 text-sm text-slate-400">
            Equipment Details
        </p>
    </div>

    <dl class="grid gap-6 sm:grid-cols-2">
        <div>
            <dt class="text-sm text-slate-400">Name</dt>
            <dd class="mt-1 text-white">{equipment.name}</dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">Category</dt>
            <dd class="mt-1 text-white">
                {categoryLabels[equipment.category]}
            </dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">VIN</dt>
            <dd class="mt-1 text-white">
                {equipment.vin ?? 'Not provided'}
            </dd>
        </div>

        <div>
            <dt class="text-sm text-slate-400">Equipment ID</dt>
            <dd class="mt-1 break-all font-mono text-sm text-white">
                {equipment.id}
            </dd>
        </div>
    </dl>
</div>
