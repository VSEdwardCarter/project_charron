
<script lang="ts">
  import Header from './lib/components/layout/Header.svelte'
  import Sidebar from './lib/components/layout/Sidebar.svelte'
  import FleetPage from "./features/fleet/FleetPage.svelte"
  import CargoPage from './features/cargo/CargoPage.svelte'

  import {
    titles,
    descriptions,
    type Section,
  } from './lib/navigation/sections'

  let activeSection: Section = $state('overview')

  function navigate(section: Section): void {
    activeSection = section
  }
</script>

<svelte:head>
  <title>CHARRON | Logistics Operations</title>
  <meta
          name="description"
          content="CHARRON logistics operations platform"
  />
</svelte:head>

<div class="min-h-screen bg-slate-950 text-slate-100">
  <Header />

  <div class="flex min-h-[calc(100vh-4rem)]">
    <Sidebar
            {activeSection}
            onNavigate={navigate}
    />

    <main class="min-w-0 flex-1 p-8">
      <div class="mx-auto max-w-6xl">
        <h2 class="text-3xl font-semibold tracking-tight">
          {titles[activeSection]}
        </h2>

        <p class="mt-2 text-slate-400">
          {descriptions[activeSection]}
        </p>

        <div
                class="mt-8 rounded-xl border border-slate-800 bg-slate-900 p-6"
        >
          {#if activeSection === 'overview'}
            <h3 class="text-lg font-medium">
              Welcome to CHARRON
            </h3>
            <p class="mt-2 text-sm text-slate-400">
              Your logistics operations workspace is ready.
            </p>
          {:else if activeSection === 'fleet'}
            <FleetPage />
          {:else if activeSection === 'cargo'}
            <CargoPage />
          {:else}
            <h3 class="text-lg font-medium">
              Movement Management
            </h3>
            <p class="mt-2 text-sm text-slate-400">
              Movement workflows will be implemented incrementally.
            </p>
          {/if}
        </div>
      </div>
    </main>
  </div>
</div>
