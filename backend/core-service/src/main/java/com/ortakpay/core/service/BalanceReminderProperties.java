package com.ortakpay.core.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.jobs.balance-reminder")
public record BalanceReminderProperties(@DefaultValue("3") int reminderIntervalDays) {}
