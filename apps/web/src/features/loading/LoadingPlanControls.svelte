
<script lang="ts">
    import { onMount } from 'svelte'

    import {
        createLoadingPlan,
        getMovementLoadingPlans,
        type LoadingPlan
    } from '../../lib/api/loadingPlans'

    import LoadingPlanCargoControls from './LoadingPlanCargoControls.svelte'

    interface Props {
        movementId: string
        equipmentId: string
        cargoSpaceId: string
    }

    let {
        movementId,
        equipmentId,
        cargoSpaceId
    }: Props = $props()

    let existingPlan: LoadingPlan | null = $state(null)

    let loading = $state(true)
    let saving = $state(false)

    let error: string | null = $state(null)
    let success: string | null = $state(null)

    async function loadExistingPlan() {
        loading = true
        error = null

        try {
            const plans = await getMovementLoadingPlans(movementId)

            existingPlan =
                plans.find(
                    (plan) =>
                        plan.cargoSpaceId === cargoSpaceId &&
                        plan.equipmentId === equipmentId
                ) ?? null
        } catch (err) {
            error =
                err instanceof Error
                    ? err.message
                    : 'Unable to retrieve loading plans'
        } finally {
            loading = false
        }
    }

    async function savePlan() {
        if (saving || loading || existingPlan) return

        saving = true
        error = null
        success = null

        try {
            const plan = await createLoadingPlan(movementId, {
                equipmentId,
                cargoSpaceId
            })

            existingPlan = plan
            success = 'Loading plan created successfully.'
        } catch (err) {
            const saveError =
                err instanceof Error
                    ? err.message
                    : 'Unable to create loading plan'

            // Refresh in case another request created the plan.
            await loadExistingPlan()

            error = saveError
        } finally {
            saving = false
        }
    }

    onMount(() => {
        void loadExistingPlan()
    })
</script>

<section class="space-y-4 rounded-lg border p-4">
    <div>
        <h3 class="text-lg font-semibold">
            Movement Loading Plan
        </h3>

        <p class="text-sm opacity-70">
            Manage the loading plan associated with this
            movement, equipment, and cargo space.
        </p>
    </div>

    {#if loading}
        <p class="text-sm">
            Checking existing loading plans...
        </p>

    {:else if existingPlan}
        <div class="space-y-3">
            <div class="space-y-2">
                <p class="text-sm font-medium text-green-600">
                    Loading plan record exists
                </p>

                <p class="text-sm">
                    <strong>Plan ID:</strong>
                    {existingPlan.id}
                </p>

                <p class="text-sm">
                    <strong>Created:</strong>
                    {new Date(existingPlan.createdAt).toLocaleString()}
                </p>
            </div>

            <div class="border-t pt-4">
                <LoadingPlanCargoControls
                        planId={existingPlan.id}
                />
            </div>

            <p class="text-xs opacity-70">
                Cargo assignments are planned quantities.
                They do not confirm physical loading.
                Calculated 3D placements are not yet persisted.
            </p>
        </div>

    {:else}
        <div class="space-y-3">
            <p class="text-sm opacity-70">
                No loading plan exists for this equipment
                and cargo space.
            </p>

            <button
                    type="button"
                    onclick={savePlan}
                    disabled={saving}
                    class="rounded-md border px-4 py-2 font-medium disabled:opacity-50"
            >
                {saving ? 'Saving...' : 'Create Loading Plan'}
            </button>
        </div>
    {/if}

    {#if success}
        <p class="text-sm text-green-600" role="status">
            {success}
        </p>
    {/if}

    {#if error}
        <div class="space-y-2">
            <p class="text-sm text-red-600" role="alert">
                {error}
            </p>

            <button
                    type="button"
                    onclick={loadExistingPlan}
                    disabled={loading || saving}
                    class="rounded-md border px-3 py-1 text-sm"
            >
                Retry
            </button>
        </div>
    {/if}
</section>
