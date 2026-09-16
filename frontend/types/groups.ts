// Type aliases into the OpenAPI-generated schema - see docs/adr/0012 and
// types/auth.ts for why these are aliases, not hand-written interfaces.
import type { components } from "@/types/api-generated";

export type GroupSummary = components["schemas"]["GroupSummaryResponse"];
export type Group = components["schemas"]["GroupResponse"];
export type CreateGroupRequest = components["schemas"]["CreateGroupRequest"];
export type Member = components["schemas"]["MemberResponse"];
export type Expense = components["schemas"]["ExpenseResponse"];
export type ExpensePage = components["schemas"]["PagedModelExpenseResponse"];
export type Balance = components["schemas"]["BalanceResponse"];
export type CreateExpenseRequest = components["schemas"]["CreateExpenseRequest"];
export type ParticipantInput = components["schemas"]["ParticipantInput"];
export type SplitType = components["schemas"]["CreateExpenseRequest"]["splitType"];
export type CreateSettlementRequest = components["schemas"]["SettlementRequest"];
export type Settlement = components["schemas"]["SettlementResponse"];
