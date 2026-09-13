# 0010. AFTER_COMMIT event publishing as a lightweight outbox stand-in

Status: Accepted

## Context

Writing to Postgres and publishing to RabbitMQ are two separate systems with no
shared transaction - there is no way to make "save the Expense" and "publish
ExpenseCreated" atomic against each other (the classic dual-write problem). If
the DB write commits but the publish never happens (or the reverse), the two
systems drift out of sync. Publishing inline, inside the same `@Transactional`
service method that does the DB write, is the naive approach and is actively
wrong here: if the publish happens before commit and the transaction then rolls
back, a message goes out for a change that never actually happened.

## Decision

Service methods publish an internal domain event via `ApplicationEventPublisher`
(e.g. `ExpenseCreatedInternalEvent`) as their last step. `EventPublisherListener`
receives it via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`,
which Spring guarantees only runs after the surrounding transaction has
successfully committed, and only then maps it to the wire-format message and
calls `rabbitTemplate.convertAndSend(...)`.

## Consequences

- Rolled-back transactions never produce a message - AFTER_COMMIT listeners
  simply never fire for a transaction that doesn't commit. This is a real,
  meaningful guarantee, not a "mostly works."
- It is not a complete guarantee: there is still a small window between the DB
  commit finishing and the RabbitMQ publish call actually succeeding. If the
  process crashes in that window (or the broker is unreachable at that exact
  moment), the message is lost even though the underlying DB change is
  permanent - the two systems can still drift, just far less often and only
  under a narrow failure mode instead of on every rollback.
- The complete fix - a transactional outbox table written in the *same*
  transaction as the business data, drained by a separate poller or CDC
  process - is deliberately deferred to Faz 9. This AFTER_COMMIT approach is
  the "good but not perfect" middle step: it removes the worst failure mode
  (publishing for work that got rolled back) with a few lines of Spring
  plumbing, at the cost of not closing the crash-in-between window that a real
  outbox would.
