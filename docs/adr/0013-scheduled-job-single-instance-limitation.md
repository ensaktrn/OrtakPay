# 0013. `@Scheduled` balance-reminder job assumes a single core-service instance

Status: Accepted

## Context

`BalanceReminderJob` uses Spring's `@Scheduled(cron = ...)` to run
`BalanceReminderService.sendDueReminders()` once a day. `@Scheduled` is a
plain in-JVM timer - it has no concept of other instances of the same
application. If core-service is ever run as more than one replica (e.g.
behind a load balancer, or during a rolling deployment where old and new
pods briefly overlap), **every** instance runs its own copy of the cron
trigger, independently.

## Decision

Ship `@Scheduled` as-is, with no distributed coordination. core-service is
deployed as a single instance for this project (see docker-compose.yml -
one `core-service` service, not a replica set), so there is currently no
scenario in which two instances' schedulers can actually race. A proper
fix - a distributed lock such as [ShedLock](https://github.com/lukas-krecan/ShedLock)
backed by a row in Postgres, so only one instance's trigger actually
executes the job body on any given run - is deliberately not added now,
since it would be complexity with no present payoff.

## Consequences

- If core-service is ever horizontally scaled (multiple replicas running
  the same JVM code) without addressing this first, every replica's
  `BalanceReminderJob` fires at the same cron tick, and `findDueReminders()`
  race to read the same set of "due" balances before any of them has
  written back `lastReminderSentAt`. The practical effect: a user could
  receive the same reminder N times (once per replica) instead of once.
- This is not a data-corruption risk - `markReminderSent`/`save` on the same
  row from multiple instances is just repeated writes of the same value,
  and `Balance`'s existing `@Version` optimistic lock still prevents a lost
  concurrent update to `netAmount` itself. The failure mode is purely
  "annoying duplicate notification," not incorrect balances.
- Before this service is horizontally scaled in a real deployment, add
  ShedLock (or an equivalent DB-based lock) around
  `BalanceReminderJob.run()` so only one instance's trigger actually
  executes per scheduled tick.
