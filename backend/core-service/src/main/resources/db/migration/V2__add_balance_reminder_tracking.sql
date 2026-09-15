-- NULL means "never reminded" - distinguishing that from "reminded a long
-- time ago" is exactly what BalanceReminderService's due-check needs.
ALTER TABLE balances ADD COLUMN last_reminder_sent_at TIMESTAMPTZ NULL;
