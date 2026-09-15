import { defineConfig, devices } from "@playwright/test";

// No webServer entry: these tests hit a stack the developer starts
// themselves (npm run dev + docker-compose up core-service and its
// dependencies) - see e2e/README.md. Auto-starting either from here would
// hide real startup failures behind Playwright's own timeout instead of
// surfacing them plainly.
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  retries: 0,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
