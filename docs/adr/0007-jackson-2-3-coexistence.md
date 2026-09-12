# 0007. Jackson 2/3 coexistence: local ObjectMapper for auth/error responses

Status: Accepted

## Context

Spring Boot 4's default JSON stack is Jackson 3 (`tools.jackson.databind.json.JsonMapper`);
it no longer auto-configures a classic Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper`
bean. The `jjwt-jackson` dependency (added for JWT support), however, pulls in
Jackson 2 transitively, so the classic `ObjectMapper` class is still present
on the classpath even though Spring no longer wires up a bean for it.

## Decision

`JwtAuthenticationEntryPoint` and the auth integration test deliberately
instantiate their own local `new ObjectMapper()` (Jackson 2) instead of
injecting a Spring-managed bean. This is safe specifically because the only
things they ever serialize — a plain `ProblemDetail` and simple request
DTOs/response trees with no date/time fields — need no custom module
registration to serialize correctly either way.

## Consequences

- This pattern must stay confined to these narrow, module-free cases. It is
  not a general answer to "how do we do JSON in this codebase."
- Any future JSON serialization need (e.g. an AI-assisted natural-language
  expense feature, per the Faz roadmap's stretch goals) must inject Spring's
  auto-configured `JsonMapper` (Jackson 3) bean, not add another local
  `new ObjectMapper()`. A second ad hoc instance would silently reintroduce
  the two-stack split this ADR exists to contain.
- If a genuine need to bridge Jackson 2 and Jackson 3 output arises, that
  should be solved deliberately (e.g. a shared conversion utility), not by
  more one-off `ObjectMapper` instances.
