# ADR-001: Initial Application Architecture

**Status:** Accepted
**Project:** CHARRON

## Context

CHARRON is a commercial logistics orchestration platform supporting cargo, fleet resources, loading, shipments, dispatch, and movement history.

The platform requires transactional correctness, extensible domain modeling, and a foundation for future intelligent logistics orchestration.

## Decision

CHARRON will initially use a modular monolith with domain-oriented packages.

### Technology

- Backend: Java 21 / Spring Boot
- Primary database: PostgreSQL
- Frontend: Svelte / TypeScript (provisional)
- Build system: Gradle
- Application architecture: Modular monolith

### Initial Domains

- Fleet and equipment
- Cargo and handling units
- Shipments and locations
- Loading and load plans
- Dispatch and movements

Each domain will organize implementation code into API, application, domain, and infrastructure layers.

### Architectural Rules

1. Business invariants belong in the domain layer.
2. Application services coordinate use cases and transactions.
3. API controllers handle transport concerns rather than business decisions.
4. Infrastructure implements persistence and external integrations.
5. Cross-domain interactions must use explicit application interfaces or contracts rather than unrestricted access to another domain's internals.
6. PostgreSQL remains the authoritative operational system of record.
7. Graph-based analysis and optimization may be added later without replacing authoritative business operations.
8. Future optimization recommendations must pass operational validation before execution.

## Consequences

This architecture supports incremental development, clear domain ownership, and transactional workflows without introducing unnecessary distributed infrastructure.

The initial modules remain within one deployable Spring Boot application. Module boundaries are architectural conventions until supported by appropriate tests or tooling.

Graph databases, distributed services, and optimization components remain deferred until justified by product requirements.