# 0012. Manually-regenerated OpenAPI types, not a build-time step

Status: Accepted

## Context

The frontend's request/response shapes (`RegisterRequest`, `LoginRequest`,
`AuthResponse`, `UserResponse`, ...) need to match core-service's DTOs
exactly. Hand-written TypeScript interfaces (Faz F2) silently drift the
moment a backend field is renamed, added, or removed - nothing catches the
mismatch until a request fails at runtime. `openapi-typescript` can generate
those types directly from core-service's own `/v3/api-docs`, which is the
actual source of truth.

The generation could run two ways: as a `predev`/`prebuild` script that
regenerates `types/api-generated.d.ts` automatically every time, or as a
manually-invoked script the developer runs on demand.

## Decision

`npm run generate:types` is a manual script
(`openapi-typescript http://localhost:8080/v3/api-docs -o types/api-generated.d.ts`).
It is not wired into `dev`, `build`, or CI. The generated file is committed
to the repo like any other source file, and `types/auth.ts` re-exports the
DTOs it needs as aliases into `components["schemas"][...]`.

Not automating it is deliberate for this project's current stage:
core-service must be running and reachable at `localhost:8080` for the
script to work at all, which build-time and CI steps can't assume (CI in
particular doesn't run the full docker-compose stack, so a `prebuild` hook
here would just fail every build). A manual script keeps the dependency
explicit instead of hiding a network requirement inside `npm run build`.

## Consequences

- Types are exactly as accurate as the last time someone ran the script -
  there is a real drift risk if a backend DTO changes and nobody
  regenerates before the frontend is next built or deployed. This is the
  main tradeoff being accepted here.
- Because the generated file is committed, a backend field rename shows up
  as a normal, reviewable diff in `types/api-generated.d.ts` in the same PR
  that changes the backend - *if* the author remembers to run the script.
  Nothing currently enforces that they did.
- Stretch goal: a CI check that re-runs `generate:types` against a
  docker-compose-started core-service and fails the build if the committed
  `api-generated.d.ts` doesn't match what gets freshly generated, catching
  the "forgot to regenerate" case instead of relying on discipline.
