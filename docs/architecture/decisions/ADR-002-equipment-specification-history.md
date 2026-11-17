# ADR-002: Equipment Specification Provenance and Modification History

**Status:** Accepted

**Project:** CHARRON

## Context

CHARRON manages physical equipment whose characteristics and operational capabilities may change over time.

Equipment may be modified through component installation, removal, replacement, or structural alteration.

Manufacturer-reported specifications may also differ from the equipment's as-delivered configuration.

CHARRON must preserve historical accuracy while supporting current equipment validation, loading operations, and dispatch.

## Decision

CHARRON will distinguish three representations of equipment specifications:

1. **Specification baseline:** Original manufacturer-reported or documented as-delivered specifications, including their sources.
2. **Modification history:** Recorded changes affecting an equipment asset's physical characteristics, capabilities, or applicable ratings.
3. **Effective specifications:** Specifications resolved for a particular equipment configuration and point in time.

### Specification Baselines

- Retain original specification records and their provenance.
- Distinguish manufacturer-reported data from verified as-delivered measurements.
- Preserve corrections as traceable revisions rather than silently overwriting historical information.
- Record verification status and source information.

### Equipment Modifications

- Represent modifications separately from equipment identity.
- Track installations, removals, replacements, and corrections.
- Associate modifications with the affected equipment and configuration.
- Preserve effective dates and historical records.
- Support both numerical adjustments and verified replacement measurements.
- Do not assume all modifications can be represented by additive adjustments.

### Effective Specifications

- Resolve effective specifications using the applicable baseline and verified changes.
- Preserve original specifications during resolution.
- Distinguish calculated values from verified measurements and certified ratings.
- Reject inconsistent or invalid resolved values.
- Do not automatically treat a calculated payload allowance as an authorized operational loading limit.

### Operational History

- Retain the equipment configuration and applicable specification version or snapshot used for significant movements.
- Prevent subsequent equipment modifications from retroactively changing historical movement records.
- Preserve sufficient provenance to explain the specifications used in operational decisions.

## Implementation Direction

- PostgreSQL will remain the authoritative operational system of record.
- Equipment identity, specification baselines, modification records, and configuration history will have distinct domain responsibilities.
- Domain resolvers will calculate applicable effective specifications without mutating authoritative baseline records.
- Persistence and audit mechanisms will be implemented incrementally.
- Initial weight adjustment logic is a limited foundation, not a complete modification-history implementation.

## Consequences

This decision increases domain and persistence complexity but supports operational traceability, reliable dispatch validation, and future equipment lifecycle management.

The initial MVP may implement a limited subset of modification types while preserving the architectural distinction between baseline, modification, and effective specifications.