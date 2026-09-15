package com.ortakpay.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin cron wrapper - the actual due-reminder logic lives in
 * BalanceReminderService so it can be called directly (bypassing the cron
 * schedule) from tests and, if ever needed, from an admin endpoint.
 */
@Component
@RequiredArgsConstructor
public class BalanceReminderJob {

    private final BalanceReminderService balanceReminderService;

    @Scheduled(cron = "${app.jobs.balance-reminder.cron:0 0 9 * * *}")
    public void run() {
        balanceReminderService.sendDueReminders();
    }
}
