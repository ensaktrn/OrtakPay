# Architecture Decision Records

Short records of the non-obvious technical decisions made on this project —
why, not just what. See [AGENTS.md](../../AGENTS.md) and
[ARCHITECTURE.md](../../ARCHITECTURE.md) for the broader project context
these decisions sit inside.

| ADR | Decision |
|---|---|
| [0001](./0001-monorepo-with-independent-services.md) | Monorepo, but no shared JAR/library between services — each stays independently deployable |
| [0002](./0002-rabbitmq-over-kafka.md) | RabbitMQ over Kafka — reliable task delivery is enough, no replay/throughput need |
| [0003](./0003-uuid-primary-keys.md) | UUID primary keys — no cross-service ID collision risk |
| [0004](./0004-materialized-balance-with-optimistic-locking.md) | Materialized `Balance` table + optimistic locking, not on-the-fly aggregation or pessimistic locking |
| [0005](./0005-timestamptz-and-instant.md) | `TIMESTAMPTZ` columns mapped to `java.time.Instant`, not `TIMESTAMP`/`LocalDateTime` |
| [0006](./0006-csrf-disabled-for-stateless-api.md) | CSRF disabled — safe because the stateless JWT API has no cookie/session for a browser to attach automatically |

## Adding a new ADR

Number sequentially, keep it to half a page: **Context** (the problem/forces),
**Decision** (what was chosen), **Consequences** (tradeoffs accepted, not just
benefits). Start with `Status: Accepted` under the title unless it's still
being discussed.
