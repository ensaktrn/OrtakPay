# 0002. RabbitMQ over Kafka for inter-service messaging

Status: Accepted

## Context

`core-service` needs to notify `notification-service` of domain events (group
invites, debt reminders, expense created) without a synchronous REST call
between them. The messaging technology needs to support reliable at-least-once
delivery of discrete tasks; there is no requirement to replay historical
events or support multiple independent consumer groups reading the same
stream.

## Decision

Use RabbitMQ (via Spring AMQP) instead of Kafka.

## Consequences

- Simpler operational footprint (one broker container, no ZooKeeper/KRaft,
  no partition/offset management) — appropriate for a two-consumer,
  low-throughput system.
- Spring AMQP's `@RabbitListener` model is a good, focused surface for
  learning reliable task delivery, acknowledgements, and retry/DLQ patterns.
- Explicit tradeoff: no event log / replay. If a new service later needs to
  reprocess historical events, or throughput requirements grow well beyond
  "one notification per domain event", this decision should be revisited.
