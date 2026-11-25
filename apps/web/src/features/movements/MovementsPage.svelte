
<script lang="ts">
    import {
        type Movement
    } from '../../lib/api/movements'

    import MovementList
        from './MovementList.svelte'

    import MovementCreationForm
        from './MovementCreationForm.svelte'

    import MovementWorkspace
        from './MovementWorkspace.svelte'

    let selectedMovement: Movement | null = $state(null)
    let movementRefreshKey = $state(0)

    function handleMovementCreated(): void {
        selectedMovement = null
        movementRefreshKey++
    }

    function openMovement(movement: Movement): void {
        selectedMovement = movement
    }

    function closeWorkspace(): void {
        selectedMovement = null

        // Refresh the movement list when returning.
        movementRefreshKey++
    }
</script>

{#if selectedMovement}

    <!-- Dedicated Movement Workspace -->

    {#key selectedMovement.id}
        <MovementWorkspace
                movement={selectedMovement}
                onBack={closeWorkspace}
        />
    {/key}

{:else}

    <!-- Movement List and Creation -->

    <div class="space-y-6">

        <div>
            <h2 class="text-2xl font-semibold text-white">
                Movement Management
            </h2>

            <p class="mt-2 text-sm text-slate-400">
                Create transportation movements and manage
                their operational workflows.
            </p>
        </div>

        <!-- Create Movement -->

        <MovementCreationForm
                onCreated={handleMovementCreated}
        />

        <hr class="border-slate-800" />

        <!-- Browse Movements -->

        {#key movementRefreshKey}
            <MovementList
                    onSelectMovement={openMovement}
            />
        {/key}

    </div>

{/if}
