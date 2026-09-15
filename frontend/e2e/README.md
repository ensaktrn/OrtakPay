# E2E tests (Playwright)

These tests drive the real app against a real backend - they are not run in
CI (no docker-compose stack is available there) and are meant to be run
locally before pushing a change to a page or mutation hook.

## Prerequisites

```bash
# from repo root
docker-compose up -d postgres-core rabbitmq core-service

# from frontend/
npm run dev
```

## Running

```bash
npx playwright test                                    # all specs
npx playwright test e2e/settlement.spec.ts --reporter=list
```

First run only: `npx playwright install chromium` downloads the browser
binary Playwright drives.
