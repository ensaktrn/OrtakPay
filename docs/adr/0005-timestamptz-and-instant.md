# 0005. TIMESTAMPTZ columns mapped to java.time.Instant

Status: Accepted

## Context

While building the Faz 1 schema, the `V1__create_core_schema.sql` migration
originally declared plain `TIMESTAMP` (no time zone) columns for
`created_at`/`updated_at`/`joined_at`/`settled_at`, mapped from entities using
`java.time.LocalDateTime`. This was caught as a latent schema mismatch:
Hibernate 6+'s default JDBC mapping for `java.time.Instant` on the PostgreSQL
dialect is `TIMESTAMPTZ`, not `TIMESTAMP` — so the instant a column type and
its entity's Java type disagree on time-zone-awareness, `ddl-auto: validate`
is one dependency-version bump away from failing schema validation, or worse,
silently storing wall-clock time with an assumed-but-unenforced zone.

## Decision

All timestamp columns are `TIMESTAMPTZ` in the Flyway migration, mapped as
`java.time.Instant` on the entities (`BaseAuditableEntity`, `GroupMember`,
`Settlement`) — not `LocalDateTime`/`TIMESTAMP`. No explicit
`@Column(columnDefinition = "TIMESTAMPTZ")` is needed: this is simply
Hibernate's own default mapping for `Instant`, so entity and schema agree
without overriding anything.

## Consequences

- Every stored timestamp is an unambiguous point in time (UTC internally),
  regardless of the database server's or any client's local time zone —
  the correct default for a system where `core-service` and
  `notification-service` could run in different environments.
- Hibernate schema validation (`ddl-auto: validate`) matches the migration
  exactly, with no type-mismatch warnings at startup.
- Displaying a timestamp in a user's local time zone is a presentation-layer
  concern (not yet built) — `Instant` carries no zone/offset of its own, so
  the UI or API layer must apply one when formatting for display.
