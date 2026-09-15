import { test, expect, type APIRequestContext } from "@playwright/test";

const API_BASE = "http://localhost:8080";
const PASSWORD = "smoke-test-password-123";

interface Registered {
  id: string;
  email: string;
  displayName: string;
}

async function apiPost<T>(request: APIRequestContext, path: string, data: unknown, token?: string): Promise<T> {
  const response = await request.post(`${API_BASE}${path}`, {
    data,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok()) {
    throw new Error(`${path} -> ${response.status()}: ${await response.text()}`);
  }
  const body = await response.text();
  return body ? (JSON.parse(body) as T) : (undefined as T);
}

/**
 * Exercises the same flow verified manually during Faz F6: a 40 TL expense
 * split between two members leaves the payer at +20 (alacaklı) and the
 * other at -20 (borçlu); the debtor then records a 10 TL settlement to the
 * creditor and both balances must move to the mathematically correct new
 * values, with NO page reload (pure invalidateQueries refetch - see
 * useCreateSettlement.ts).
 *
 * Requires the dev stack to already be running: `npm run dev` (port 3000)
 * and docker-compose's postgres-core/rabbitmq/core-service (port 8080). Not
 * run in CI - see e2e/README.md for why.
 */
test("debtor records a settlement and both balances update without a reload", async ({ page, request }) => {
  const runId = Date.now();
  const creditorEmail = `settle-creditor-${runId}@example.com`;
  const debtorEmail = `settle-debtor-${runId}@example.com`;

  const creditor = await apiPost<Registered>(request, "/api/auth/register", {
    email: creditorEmail,
    password: PASSWORD,
    displayName: "Creditor",
  });
  const debtor = await apiPost<Registered>(request, "/api/auth/register", {
    email: debtorEmail,
    password: PASSWORD,
    displayName: "Debtor",
  });
  const { token: creditorToken } = await apiPost<{ token: string }>(request, "/api/auth/login", {
    email: creditorEmail,
    password: PASSWORD,
  });
  const { token: debtorToken } = await apiPost<{ token: string }>(request, "/api/auth/login", {
    email: debtorEmail,
    password: PASSWORD,
  });
  const group = await apiPost<{ id: string }>(request, "/api/groups", { name: "Settlement Test Group" }, creditorToken);
  await apiPost(request, `/api/groups/${group.id}/members`, { email: debtorEmail }, creditorToken);
  await apiPost(
    request,
    `/api/groups/${group.id}/expenses`,
    {
      paidBy: creditor.id,
      amount: 40,
      description: "Shared Dinner",
      splitType: "EQUAL",
      participants: [{ userId: creditor.id }, { userId: debtor.id }],
    },
    creditorToken,
  );

  // Login as the debtor - they're the one who will record "I paid".
  await page.goto("/login");
  await page.fill("#email", debtorEmail);
  await page.fill("#password", PASSWORD);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/groups");

  await page.click(`a[href="/groups/${group.id}"]`);
  await page.waitForURL(`**/groups/${group.id}`);

  const balancesSection = page.locator("section", { hasText: "Bakiyeler" });
  const creditorRow = balancesSection.locator("li", { hasText: "Creditor" });
  const debtorRow = balancesSection.locator("li", { hasText: "Debtor" });

  await expect(creditorRow).toContainText("alacaklı");
  await expect(creditorRow).toContainText("20.00");
  await expect(debtorRow).toContainText("borçlu");
  await expect(debtorRow).toContainText("-20.00");

  await page.click(`a[href="/groups/${group.id}/settlements/new"]`);
  await page.waitForURL("**/settlements/new");

  // The select excludes the logged-in user - only the creditor can appear.
  await expect(page.locator("#toUserId option")).toHaveCount(1);
  await expect(page.locator("#toUserId option")).toHaveAttribute("value", creditor.id);

  await page.fill("#amount", "10");
  await page.click('button[type="submit"]');

  // Client-side navigation back to the group detail - not a hard reload -
  // is what proves the balances below are refreshed purely by
  // invalidateQueries, not by the browser re-fetching the whole page.
  await page.waitForURL(`**/groups/${group.id}`);

  await expect(creditorRow).toContainText("alacaklı");
  await expect(creditorRow).toContainText("10.00");
  await expect(debtorRow).toContainText("borçlu");
  await expect(debtorRow).toContainText("-10.00");

  // Backend defense: the UI's select already excludes yourself, but the
  // server must reject a self-settlement independently of that UI choice.
  const selfSettlement = await request.post(`${API_BASE}/api/groups/${group.id}/settlements`, {
    data: { toUserId: debtor.id, amount: 5 },
    headers: { Authorization: `Bearer ${debtorToken}` },
  });
  expect(selfSettlement.status()).toBe(400);
});
