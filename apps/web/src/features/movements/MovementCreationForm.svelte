
<script lang="ts">
    import {
        createMovement
    } from '../../lib/api/movements'

    interface Props {
        onCreated: () => void
    }

    let { onCreated }: Props = $props()

    let reference = $state('')
    let origin = $state('')
    let destination = $state('')
    let plannedDeparture = $state('')
    let plannedArrival = $state('')

    let submitting = $state(false)
    let error: string | null = $state(null)
    let success: string | null = $state(null)

    async function submitMovement(
        event: SubmitEvent
    ): Promise<void> {
        event.preventDefault()

        if (submitting) return

        error = null
        success = null

        const departure = new Date(plannedDeparture)
        const arrival = new Date(plannedArrival)

        if (
            !Number.isFinite(departure.getTime()) ||
            !Number.isFinite(arrival.getTime())
        ) {
            error = 'Valid departure and arrival dates are required.'
            return
        }

        if (arrival <= departure) {
            error = 'Planned arrival must follow departure.'
            return
        }

        submitting = true

        try {
            const movement = await createMovement({
                reference: reference.trim(),
                origin: origin.trim(),
                destination: destination.trim(),
                plannedDeparture: departure.toISOString(),
                plannedArrival: arrival.toISOString()
            })

            success = `Movement ${movement.reference} created.`

            reference = ''
            origin = ''
            destination = ''
            plannedDeparture = ''
            plannedArrival = ''

            onCreated()
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Unable to create movement'
        } finally {
            submitting = false
        }
    }
</script>

<section class="space-y-4">
    <div>
        <h3 class="text-lg font-semibold text-white">
            Create Movement
        </h3>

        <p class="mt-1 text-sm text-slate-400">
            Register a new transportation movement.
        </p>
    </div>

    <form
            onsubmit={submitMovement}
            class="space-y-4 rounded-xl border border-slate-800 p-5"
    >
        <div class="grid gap-4 md:grid-cols-2">

            <div class="space-y-2">
                <label for="movement-reference"
                       class="block text-sm text-slate-300">
                    Movement Reference
                </label>

                <input
                        id="movement-reference"
                        type="text"
                        bind:value={reference}
                        maxlength="100"
                        required
                        placeholder="MOV-002"
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2 text-white"
                />
            </div>

            <div class="space-y-2">
                <label for="movement-origin"
                       class="block text-sm text-slate-300">
                    Origin
                </label>

                <input
                        id="movement-origin"
                        type="text"
                        bind:value={origin}
                        required
                        placeholder="Denver, CO"
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2 text-white"
                />
            </div>

            <div class="space-y-2">
                <label for="movement-destination"
                       class="block text-sm text-slate-300">
                    Destination
                </label>

                <input
                        id="movement-destination"
                        type="text"
                        bind:value={destination}
                        required
                        placeholder="Dallas, TX"
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2 text-white"
                />
            </div>

            <div class="space-y-2">
                <label for="movement-departure"
                       class="block text-sm text-slate-300">
                    Planned Departure
                </label>

                <input
                        id="movement-departure"
                        type="datetime-local"
                        bind:value={plannedDeparture}
                        required
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2 text-white"
                />
            </div>

            <div class="space-y-2">
                <label for="movement-arrival"
                       class="block text-sm text-slate-300">
                    Planned Arrival
                </label>

                <input
                        id="movement-arrival"
                        type="datetime-local"
                        bind:value={plannedArrival}
                        required
                        class="w-full rounded-lg border border-slate-700
                           bg-slate-950 px-3 py-2 text-white"
                />
            </div>
        </div>

        {#if error}
            <p role="alert" class="text-sm text-red-400">
                {error}
            </p>
        {/if}

        {#if success}
            <p role="status" class="text-sm text-emerald-400">
                {success}
            </p>
        {/if}

        <div class="flex justify-end">
            <button
                    type="submit"
                    disabled={submitting}
                    class="rounded-lg bg-emerald-600 px-5 py-2.5
                       text-sm font-medium text-white
                       hover:bg-emerald-500
                       disabled:cursor-not-allowed
                       disabled:opacity-50"
            >
                {submitting ? 'Creating...' : 'Create Movement'}
            </button>
        </div>
    </form>
</section>
