package com.charron.api.movement.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ManifestStatus {

    DRAFT,
    REVIEWED,
    ISSUED,
    SUPERSEDED;

    public Set<ManifestStatus> allowedTransitions() {
        return switch (this) {

            case DRAFT -> EnumSet.of(
                    REVIEWED
            );

            case REVIEWED -> EnumSet.of(
                    ISSUED
            );

            case ISSUED -> EnumSet.of(
                    SUPERSEDED
            );

            case SUPERSEDED ->
                    EnumSet.noneOf(ManifestStatus.class);
        };
    }

    public boolean canTransitionTo(ManifestStatus target) {
        return target != null
                && allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == SUPERSEDED;
    }

    public boolean isIssued() {
        return this == ISSUED;
    }
}