
export type EquipmentCategory =
    | 'TRACTOR'
    | 'TRAILER'
    | 'STRAIGHT_TRUCK'
    | 'CARGO_VAN'
    | 'CONTAINER'
    | 'MATERIAL_HANDLING'
    | 'SPECIALIZED'
    | 'OTHER'

export interface Equipment {
    id: string
    name: string
    category: EquipmentCategory
    vin: string | null
}

export const categoryLabels: Record<EquipmentCategory, string> = {
    TRACTOR: 'Tractor',
    TRAILER: 'Trailer',
    STRAIGHT_TRUCK: 'Straight Truck',
    CARGO_VAN: 'Cargo Van',
    CONTAINER: 'Container',
    MATERIAL_HANDLING: 'Material Handling',
    SPECIALIZED: 'Specialized',
    OTHER: 'Other',
}
