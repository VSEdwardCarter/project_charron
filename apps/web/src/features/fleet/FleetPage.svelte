
<script lang="ts">
    import {
        categoryLabels,
        type Equipment,
    } from '../../lib/models/equipment'

    const equipment: Equipment[] = [
        {
            id: '1',
            name: 'Freightliner Cascadia',
            category: 'TRACTOR',
            vin: null,
        },
        {
            id: '2',
            name: 'Great Dane Dry Van',
            category: 'TRAILER',
            vin: null,
        },
        {
            id: '3',
            name: 'Ford Transit',
            category: 'CARGO_VAN',
            vin: null,
        },
    ]
</script>

<div class="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
    <div class="flex items-center justify-between border-b border-slate-800 p-6">
        <div>
            <h3 class="text-lg font-semibold">Equipment</h3>
            <p class="mt-1 text-sm text-slate-400">
                Vehicles, trailers, and operational equipment
            </p>
        </div>

        <span class="rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-300">
      {equipment.length} Assets
    </span>
    </div>

    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-800/50 text-xs uppercase tracking-wide text-slate-400">
            <tr>
                <th class="px-6 py-4 font-medium">Name</th>
                <th class="px-6 py-4 font-medium">Category</th>
                <th class="px-6 py-4 font-medium">VIN</th>
            </tr>
            </thead>

            <tbody class="divide-y divide-slate-800">
            {#each equipment as item (item.id)}
                <tr class="transition-colors hover:bg-slate-800/40">
                    <td class="px-6 py-4 font-medium text-white">
                        {item.name}
                    </td>

                    <td class="px-6 py-4 text-slate-300">
                        {categoryLabels[item.category]}
                    </td>

                    <td class="px-6 py-4 text-slate-400">
                        {item.vin ?? '—'}
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
</div>
