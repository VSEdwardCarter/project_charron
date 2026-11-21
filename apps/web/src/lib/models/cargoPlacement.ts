
export type CargoOrientation = 'ORIGINAL' | 'ROTATED_90'

export interface CargoPlacement3D {
    cargoId: string
    cargoSpaceId: string
    unitIndex: number

    xMm: number
    yMm: number
    zMm: number

    orientation: CargoOrientation

    lengthMm: number
    widthMm: number
    heightMm: number

    label?: string
}
