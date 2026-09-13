# 0009. Explicit service-layer authorization, not @PreAuthorize

Status: Accepted

## Context

Starting Faz 4, most endpoints operate on a specific group and must check
that the calling user is actually a member of it before touching any of its
data. Spring Security offers a declarative way to express this:
`@PreAuthorize("@someBean.isMember(#groupId, authentication)")` backed by a
custom `PermissionEvaluator` or a SpEL expression referencing a bean. The
alternative is what AGENTS.md already commits to for authorization generally:
explicit, resource-based checks written directly in the service layer.

## Decision

Authorization is an explicit method call, not an annotation:
`GroupAccessGuard.requireMembership(groupId, userId)` is called as the first
line of every service method that touches a specific group's data
(`ExpenseService.createExpense`/`getExpenses`, `BalanceService.getGroupBalances`,
`SettlementService.recordSettlement`, `GroupService.addMember`/`getGroupDetails`).
No `@PreAuthorize`, no custom `PermissionEvaluator`.

## Consequences

- Nothing enforces that a new service method remembers to call the guard -
  unlike AOP-based `@PreAuthorize`, there is no framework-level guarantee.
  Forgetting the call is a real, silent failure mode this design accepts;
  code review and tests are the only backstop.
- In exchange, the authorization check is just a regular method call: it
  shows up directly in the method body, is trivial to step through in a
  debugger, and is testable with a plain unit test (mock the guard, assert
  it's called) instead of a Spring context test exercising SpEL evaluation.
- Consistent with the existing convention: AGENTS.md already specifies
  resource-based authorization ("bu kullanıcı bu gruba üye mi?") lives in the
  service layer, not a security-annotation layer - this just makes that
  concrete with one reusable guard component instead of ad hoc checks
  repeated in each service.
