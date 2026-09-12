# 0003. UUID primary keys

Status: Accepted

## Context

All core domain entities (`User`, `Group`, `GroupMember`, `Expense`,
`ExpenseShare`, `Balance`, `Settlement`) need primary keys. These IDs will
appear in RabbitMQ events consumed by `notification-service`, and potentially
by other services later, so they need to be safe to generate and reference
outside the owning database's control.

## Decision

Use UUIDs (Hibernate's native `GenerationType.UUID`, mapped to PostgreSQL's
`uuid` column type) instead of auto-incrementing `BIGINT` identity columns.

## Consequences

- No cross-service ID collision risk: any service (or a future client) can
  reference an entity by ID without coordinating a shared sequence.
- IDs are not sequential, so they don't leak creation order or row counts
  to clients (a minor security/privacy plus).
- Cost: UUID primary keys are 16 bytes vs. 8 for `BIGINT`, and don't cluster
  as well on insert — acceptable at this project's scale, and a reasonable
  tradeoff against the coordination cost sequential IDs would otherwise
  impose across services.
- Row creation order is preserved separately via `created_at`
  (see [0005](./0005-timestamptz-and-instant.md)), so nothing relies on ID
  ordering.
