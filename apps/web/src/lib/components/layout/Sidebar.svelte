
<script lang="ts">
    import { navigation, type Section } from '../../navigation/sections'

    interface Props {
        activeSection: Section
        onNavigate: (section: Section) => void
    }

    let { activeSection, onNavigate }: Props = $props()
</script>

<aside
        class="w-60 shrink-0 border-r border-slate-800 bg-slate-900 p-4"
>
    <nav aria-label="Main navigation" class="space-y-2">
        {#each navigation as item}
            <button
                    type="button"
                    onclick={() => onNavigate(item.id)}
                    aria-current={activeSection === item.id ? 'page' : undefined}
                    class={[
          'w-full rounded-lg px-4 py-3 text-left text-sm font-medium transition-colors',
          activeSection === item.id
            ? 'bg-emerald-600 text-white'
            : 'text-slate-400 hover:bg-slate-800 hover:text-white',
        ].join(' ')}
            >
                {item.label}
            </button>
        {/each}
    </nav>
</aside>
