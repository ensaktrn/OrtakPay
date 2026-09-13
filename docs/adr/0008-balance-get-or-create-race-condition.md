# 0008. Balance.getOrCreate race condition left unhandled

Status: Accepted

## Context

`BalanceService.getOrCreate(group, user)` reads for an existing `Balance` row
and, if absent, inserts a new one. If two transactions run this concurrently
for the *same* group+user pair — e.g. two of a group's very first expenses,
naming the same participant, submitted at nearly the same instant — both can
read "no row exists" and both attempt to insert, so the second insert fails
against `uk_balances_group_user` with a unique constraint violation
(`DataIntegrityViolationException`), not the `ObjectOptimisticLockingFailureException`
that `@Version` protects against once a row already exists.

## Decision

Leave this race unhandled for now. `GlobalExceptionHandler` has no mapping
for it, so it would currently surface as a 500.

This is a narrow window (only a brand-new group+user pair's very first
touch) with low real-world likelihood at this project's scale — one user
manually creating a handful of test expenses, not concurrent production
traffic — so it isn't worth the added complexity yet.

## Consequences

- A rare, real concurrent-first-expense scenario would return a 500 instead
  of a clean 4xx, until this is revisited.
- Two known fixes exist for whenever this needs to be addressed: catch
  `DataIntegrityViolationException` in `getOrCreate` and retry the read (the
  loser's insert failing means a row now exists to read), or take a
  `SELECT ... FOR UPDATE` (pessimistic lock) specifically for the
  create-if-absent path — deliberately not the default locking strategy for
  this table (see [0004](./0004-materialized-balance-with-optimistic-locking.md)),
  just a targeted exception for the one code path where "does a row exist
  yet" is itself the race.
