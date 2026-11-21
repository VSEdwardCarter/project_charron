
import type { Cargo } from '../api/cargo'
import type {
    LoadingPreviewCargoItem
} from '../api/loadingPreview'

export interface LoadingSelection {
    cargoId: string
    quantity: number
}

export function isCargoLoadable(cargo: Cargo): boolean {
    return (
        cargo.lengthMm !== null &&
        cargo.widthMm !== null &&
        cargo.heightMm !== null &&
        cargo.lengthMm > 0 &&
        cargo.widthMm > 0 &&
        cargo.heightMm > 0 &&
        Number.isSafeInteger(cargo.lengthMm) &&
        Number.isSafeInteger(cargo.widthMm) &&
        Number.isSafeInteger(cargo.heightMm)
    )
}

export function buildLoadingPreviewCargo(
    catalog: Cargo[],
    selections: LoadingSelection[]
): LoadingPreviewCargoItem[] {
    const byId = new Map(catalog.map(item => [item.id, item]))

    return selections.map(selection => {
        const cargo = byId.get(selection.cargoId)

        if (!cargo) {
            throw new Error(
                `Cargo ${selection.cargoId} is not registered`
            )
        }

        if (!Number.isSafeInteger(selection.quantity) ||
            selection.quantity <= 0) {
            throw new Error(
                `Quantity for ${cargo.name} must be a positive integer`
            )
        }

        if (!isCargoLoadable(cargo)) {
            throw new Error(
                `${cargo.name} requires complete dimensions before loading`
            )
        }

        return {
            cargoId: cargo.id,
            name: cargo.name,
            quantity: selection.quantity,
            lengthMm: cargo.lengthMm!,
            widthMm: cargo.widthMm!,
            heightMm: cargo.heightMm!,
            weightGrams: cargo.weightGrams
        }
    })
}
