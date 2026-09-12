# 0001. Monorepo with independent, deployable services

Status: Accepted

## Context

OrtakPay is split into two Spring Boot services, `core-service` (users, groups,
expenses, balances, settlements) and `notification-service` (async event
consumption, notification history). They need to evolve together during
development, but at runtime they must stay two separate processes with
separate databases — `core-service`'s response time must not depend on
whether `notification-service` is up.

## Decision

Keep both services in a single git repository, but each with its own
`pom.xml`, its own base package, its own database (`core_db` / `notif_db`),
and its own Docker container. There is no shared JAR/library between them:
event DTOs are defined independently in each service, even though this means
duplicating a small DTO shape.

## Consequences

- One repo, one `AGENTS.md`/`ARCHITECTURE.md`, one commit history — simpler
  to develop and to present as a portfolio project.
- No accidental compile-time coupling: a change to `core-service` can never
  break `notification-service`'s build.
- Cost: event contract changes must be updated by hand in both services
  (no shared schema/types); this is an accepted tradeoff, not an oversight.
- Future temptation to extract a `common` module should be resisted unless
  duplication becomes a real, repeated maintenance burden — see
  [ARCHITECTURE.md](../../ARCHITECTURE.md) §4 for the same tradeoff recorded
  against a shared library.
