
export type Section =
    | 'overview'
    | 'fleet'
    | 'cargo'
    | 'movements'

export interface NavigationItem {
    id: Section
    label: string
}

export const navigation: NavigationItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'fleet', label: 'Fleet' },
    { id: 'cargo', label: 'Cargo' },
    { id: 'movements', label: 'Movements' },
]

export const titles: Record<Section, string> = {
    overview: 'Overview',
    fleet: 'Fleet',
    cargo: 'Cargo',
    movements: 'Movements',
}

export const descriptions: Record<Section, string> = {
    overview: 'Your logistics operations workspace.',
    fleet: 'Manage vehicles, trailers, and equipment.',
    cargo: 'Manage cargo and its specifications.',
    movements: 'Plan and manage cargo movements.',
}
