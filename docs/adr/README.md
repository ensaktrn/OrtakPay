# Architecture Decision Records

Short records of the non-obvious technical decisions made on this project —
why, not just what. See [AGENTS.md](../agent/AGENTS.md) and
[ARCHITECTURE.md](../agent/ARCHITECTURE.md) for the broader project context
these decisions sit inside.

| ADR | Decision |
|---|---|
| [0001](./0001-monorepo-with-independent-services.md) | Monorepo, but no shared JAR/library between services — each stays independently deployable |
| [0002](./0002-rabbitmq-over-kafka.md) | RabbitMQ over Kafka — reliable task delivery is enough, no replay/throughput need |
| [0003](./0003-uuid-primary-keys.md) | UUID primary keys — no cross-service ID collision risk |
| [0004](./0004-materialized-balance-with-optimistic-locking.md) | Materialized `Balance` table + optimistic locking, not on-the-fly aggregation or pessimistic locking |
| [0005](./0005-timestamptz-and-instant.md) | `TIMESTAMPTZ` columns mapped to `java.time.Instant`, not `TIMESTAMP`/`LocalDateTime` |
| [0006](./0006-csrf-disabled-for-stateless-api.md) | CSRF disabled — safe because the stateless JWT API has no cookie/session for a browser to attach automatically |
| [0007](./0007-jackson-2-3-coexistence.md) | Local Jackson 2 `ObjectMapper` for ProblemDetail/simple DTOs, scoped narrowly since Spring Boot 4 defaults to Jackson 3 |
| [0008](./0008-balance-get-or-create-race-condition.md) | `Balance.getOrCreate`'s first-insert race left unhandled for now — low likelihood at this scale, two known fixes if it matters later |
| [0009](./0009-explicit-service-layer-authorization.md) | Explicit `GroupAccessGuard.requireMembership()` calls in the service layer, not `@PreAuthorize` + a PermissionEvaluator |
| [0010](./0010-transactional-outbox-lite.md) | `@TransactionalEventListener(AFTER_COMMIT)` publishes events only after commit — a lightweight stand-in for a full transactional outbox, deferred to Faz 9 |
| [0011](./0011-notification-consumer-not-idempotent.md) | Notification consumers aren't idempotent against RabbitMQ's at-least-once redelivery — accepted since only a mocked log line is affected today, revisit once a real email/push provider exists |
| [0012](./0012-manual-openapi-type-regeneration.md) | Frontend API types regenerated from `/v3/api-docs` via a manual script, not a build-time/CI step — accepts drift risk since core-service isn't reachable during CI or build |
| [0013](./0013-scheduled-job-single-instance-limitation.md) | `@Scheduled` balance-reminder job has no distributed lock — accepted for now since core-service runs as a single instance; ShedLock needed before horizontal scaling |

## Adding a new ADR

Number sequentially, keep it to half a page: **Context** (the problem/forces),
**Decision** (what was chosen), **Consequences** (tradeoffs accepted, not just
benefits). Start with `Status: Accepted` under the title unless it's still
being discussed.
