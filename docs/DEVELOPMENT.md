# CHARRON — Development Standards

**Status:** Active  
**Architecture:** Modular Monolith  
**Backend:** Java 21 / Spring Boot

## 1. Development Principles

- Implement one verified increment at a time.
- Prioritize operational correctness over premature optimization.
- Separate domain logic from application frameworks.
- Maintain clear boundaries between logistics domains.
- Avoid unnecessary dependencies and distributed infrastructure.
- Preserve operational traceability.

## 2. Java Conventions

- Follow standard Java naming conventions.
- Use descriptive names representing logistics concepts.
- Organize packages by business domain.
- Keep classes focused on a single responsibility.
- Prefer constructor injection for dependencies.
- Use immutable value objects where appropriate.

## 3. Application Layers

Each business module follows:

- `api`: HTTP controllers and transport contracts.
- `application`: Use cases and transaction coordination.
- `domain`: Business models, rules, and invariants.
- `infrastructure`: Persistence and external integrations.

Domain code must not depend on Spring MVC, JPA, or infrastructure implementations.

Cross-domain access must occur through defined interfaces or application contracts.

## 4. API Standards

- Use explicit request and response DTOs.
- Validate incoming requests.
- Keep HTTP concerns outside domain models.
- Use consistent status codes and error responses.
- Do not expose persistence entities directly through APIs.
- Introduce API versioning when compatibility requirements justify it.

## 5. Persistence Standards

- PostgreSQL is the authoritative operational database.
- Manage database schema changes through versioned migrations.
- Do not use automatic schema generation as the production migration mechanism.
- Enforce critical invariants through application logic and appropriate database constraints.
- Record significant operational state transitions.
- Use transactions for operations requiring atomic changes.

## 6. Testing Standards

- Unit-test domain invariants and business rules.
- Integration-test persistence and transactional behavior.
- Test HTTP contracts and input validation.
- Include concurrency tests for resource allocation and dispatch.
- Run relevant tests before committing implementation changes.

## 7. Git Standards

- Use `main` as the primary development branch.
- Make small, focused commits.
- Use descriptive commit messages.
- Do not commit credentials, generated build artifacts, or IDE-specific files.
- Verify the working tree and relevant tests before completing milestones.

## 8. Architecture Evolution

- Begin with a modular monolith.
- Introduce additional infrastructure only when justified by requirements.
- Keep graph databases and optimization engines optional.
- Document significant architectural changes using Architecture Decision Records.
- Maintain backward compatibility when introducing integrations or evolving API contracts.

## 9. Verification

A development milestone is complete only when its implementation and expected behavior have been verified.

Documentation and architectural decisions do not constitute implemented functionality.