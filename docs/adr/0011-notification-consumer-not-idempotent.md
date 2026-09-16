# 0011. Notification consumers are not idempotent

Status: Accepted

## Context

RabbitMQ gives an **at-least-once** delivery guarantee, not exactly-once: if
`NotificationListener` processes a message (persists a `NotificationLog` row)
but crashes or loses its connection before the broker receives the ack, the
broker redelivers the same message. `NotificationListener`'s handlers have no
way to recognize "I've already handled this exact message" - each delivery
is processed as if it were new, regardless of whether it actually is.

## Decision

Ship without idempotency for now. `NotificationLog` has no unique constraint
tying a row back to a specific message delivery, and no handler checks
"does a log for this event already exist" before inserting. This gap was
called out during Faz 6 (RabbitMQ event flow) but no ADR was written for it
at the time - this one exists to close that gap rather than to introduce
new behavior.

## Consequences

- A duplicate `NotificationLog` row can appear, but only in the narrow
  window between a handler finishing its work and the broker recording the
  ack - not on every redelivery, and not under normal operation. Given this
  service only logs a mocked "notification sent" line (no real email/push
  provider yet, see the `NotificationListener` class comment), the practical
  impact today is an occasional duplicate audit-log row, not a duplicate
  message to an actual user.
- If a real email/push provider is wired in later, this stops being
  cosmetic - a duplicate delivery to `NotificationListener` would mean a
  duplicate email/push to the end user, which is a real problem.
- The fix, when needed: derive a stable message ID (RabbitMQ's message
  properties support one, or the payload can carry one) and add a UNIQUE
  constraint on it in `notification_logs`, with each handler checking "has
  this ID already been processed" before doing any work - turning a
  redelivered message into a no-op instead of a duplicate.
