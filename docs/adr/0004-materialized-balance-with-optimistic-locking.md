# 0004. Materialized Balance table with optimistic locking

Status: Accepted

## Context

Each user's net balance within a group could be computed on demand by
summing `Expense`/`ExpenseShare`/`Settlement` rows. This project's explicit
goal, though, is to demonstrate transaction-boundary and concurrency-control
skills (see [ARCHITECTURE.md](../../ARCHITECTURE.md) §3), and on-the-fly
aggregation neither creates nor exercises a real concurrency problem — two
concurrent expense creations touching the same group/user pair would simply
each run their own independent read, with no shared mutable state to
conflict over.

## Decision

Keep a separate, materialized `Balance` table (one row per `group_id` +
`user_id`, unique-constrained), updated transactionally every time an expense
or settlement affects it. Concurrency on that row is controlled with
optimistic locking (`@Version`), not pessimistic locking (`SELECT ... FOR
UPDATE`).

Optimistic over pessimistic: two users adding an expense to the *same* group
at the *same instant* is a rare event in real usage, not the common case.
Pessimistic locking would serialize every write to a group's balance rows
even when no other transaction is touching them, paying a throughput cost on
every single request to guard against a conflict that usually doesn't exist.
Optimistic locking instead only pays a cost — a retry, or surfacing a 409 —
on the rare transaction that actually loses the race, which is the correct
trade for this access pattern.

## Consequences

- Balance reads are O(1) lookups, not O(n) aggregations over expense history.
- The write path (Faz 3) must update `Balance` inside the *same* transaction
  as the `Expense`/`ExpenseShare` insert, or the two will drift.
- A concurrent conflicting update raises `OptimisticLockException`, which the
  service layer must translate into a `409 Conflict` (see
  [ARCHITECTURE.md](../../ARCHITECTURE.md) §3 sequence diagram) — the client
  is expected to retry, not the server silently.
- `net_amount` intentionally has no `CHECK` constraint: it is a signed value
  (negative = this user owes into the group), so both signs are valid.
