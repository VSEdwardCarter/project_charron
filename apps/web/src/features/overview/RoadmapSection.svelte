<script lang="ts">
    interface RoadmapCapability {
        title: string
        description: string
        release: string
    }

    const plannedCapabilities: RoadmapCapability[] = [
        {
            title: 'Personnel & Credential Management',
            description:
                'Manage drivers, employees, and contractors, including licenses, endorsements, professional qualifications, port access credentials, security clearances, availability, and assignment eligibility.',
            release: 'Future Release'
        },
        {
            title: 'Contract Opportunity Intelligence',
            description:
                'Discover private-sector logistics opportunities, evaluate organizational alignment, identify capability gaps, and discover potential teaming partners. Future expansion will include public-sector contracting opportunities.',
            release: 'Future Release'
        },
        {
            title: 'Contract Feasibility & Bid Analysis',
            description:
                'Evaluate contract requirements against available equipment, personnel, and operational capacity. Estimate resource gaps, acquisition alternatives, operating costs, break-even thresholds, and profitability to support informed bid submissions.',
            release: 'Future Release'
        },
        {
            title: 'Resource Substitution Intelligence',
            description:
                'Identify and rank alternative equipment and personnel when assigned resources become unavailable. Evaluate compatibility, capabilities, location, availability, and operational constraints while explaining substitution recommendations.',
            release: 'Future Release'
        }
    ]

    const advancedCapabilities: RoadmapCapability[] = [
        {
            title: 'Recurring Route & Distribution Scheduling',
            description:
                'Create reusable routes, recurring delivery schedules, multi-stop distribution operations, vendor delivery windows, and scheduled milk runs.',
            release: '3.x.x'
        },
        {
            title: 'Dynamic Route Capacity Management',
            description:
                'Evaluate changing weight, volume, pallet capacity, and available equipment capacity throughout multi-stop delivery and collection operations.',
            release: '3.x.x'
        },
        {
            title: 'Operational Cost & Profitability Analysis',
            description:
                'Compare anticipated and actual delivery costs, evaluate customer and vendor profitability, assess route expansion opportunities, and monitor operational margins.',
            release: '3.x.x'
        },
        {
            title: 'Financial & Accounting Interoperability',
            description:
                'Capture operational revenue, expenses, labor, fuel, equipment costs, and billable services. Support accounting-ready exports, financial reconciliation, and future integration with external accounting platforms.',
            release: '3.x.x'
        }
    ]
</script>

<section class="mt-8 space-y-8">

    <div>
        <h3 class="text-xl font-semibold text-white">
            Product Roadmap
        </h3>

        <p class="mt-2 text-sm text-slate-400">
            Planned capabilities supporting the continued evolution
            of CHARRON into an integrated logistics orchestration platform.
        </p>
    </div>

    <!-- Planned capabilities -->

    <section>
        <h4 class="text-lg font-medium text-slate-200">
            Planned Capabilities
        </h4>

        <p class="mt-1 text-sm text-slate-500">
            Future operational and business intelligence capabilities.
        </p>

        <div class="mt-4 grid gap-4 lg:grid-cols-2">

            {#each plannedCapabilities as capability (capability.title)}

                <article class="rounded-xl border border-slate-800
                                bg-slate-950/50 p-5">

                    <div class="flex flex-wrap items-center
                                justify-between gap-2">

                        <span class="rounded border border-amber-800
                                     bg-amber-950/40 px-2 py-1
                                     text-xs font-medium text-amber-400">
                            Planned
                        </span>

                        <span class="text-xs text-slate-500">
                            {capability.release}
                        </span>

                    </div>

                    <h5 class="mt-4 text-base font-semibold text-slate-200">
                        {capability.title}
                    </h5>

                    <p class="mt-2 text-sm leading-relaxed text-slate-400">
                        {capability.description}
                    </p>

                    <p class="mt-4 text-xs text-slate-500">
                        Not yet available
                    </p>

                </article>

            {/each}

        </div>
    </section>

    <!-- Advanced operational capabilities -->

    <section>
        <h4 class="text-lg font-medium text-slate-200">
            Advanced Operations
        </h4>

        <p class="mt-1 text-sm text-slate-500">
            Planned for the CHARRON 3.x.x release series.
        </p>

        <div class="mt-4 grid gap-4 lg:grid-cols-2">

            {#each advancedCapabilities as capability (capability.title)}

                <article class="rounded-xl border border-slate-800
                                bg-slate-950/50 p-5">

                    <div class="flex flex-wrap items-center
                                justify-between gap-2">

                        <span class="rounded border border-amber-800
                                     bg-amber-950/40 px-2 py-1
                                     text-xs font-medium text-amber-400">
                            Planned
                        </span>

                        <span class="text-xs text-slate-500">
                            Version {capability.release}
                        </span>

                    </div>

                    <h5 class="mt-4 text-base font-semibold text-slate-200">
                        {capability.title}
                    </h5>

                    <p class="mt-2 text-sm leading-relaxed text-slate-400">
                        {capability.description}
                    </p>

                    <p class="mt-4 text-xs text-slate-500">
                        Not yet available
                    </p>

                </article>

            {/each}

        </div>
    </section>

    <p class="border-t border-slate-800 pt-4 text-xs text-slate-500">
        Roadmap capabilities represent planned product development.
        Scope and release sequencing are subject to change.
    </p>

</section>