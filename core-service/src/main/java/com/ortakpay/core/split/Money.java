package com.ortakpay.core.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cent-level rounding helpers shared by the split strategies that need to
 * guarantee shares sum EXACTLY to the expense total, down to the last cent.
 */
final class Money {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private Money() {}

    static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    /**
     * Splits totalCents into n equal-as-possible parts; any leftover cent(s) go to
     * the first participants, in list order.
     */
    static List<Long> distributeEqually(long totalCents, int n) {
        long base = totalCents / n;
        long remainder = totalCents % n;
        List<Long> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(base + (i < remainder ? 1 : 0));
        }
        return result;
    }

    /**
     * Splits totalCents proportionally to the given percentages (which must already
     * be validated to sum to 100). Each share is floored to whole cents first, which
     * guarantees the sum of floors never exceeds totalCents; the resulting leftover
     * (always in [0, n)) is then distributed one cent at a time to the first
     * participants, in list order - the same rule as {@link #distributeEqually}.
     */
    static List<Long> distributeProportionally(long totalCents, List<BigDecimal> percentages) {
        int n = percentages.size();
        long[] base = new long[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            base[i] = BigDecimal.valueOf(totalCents)
                    .multiply(percentages.get(i))
                    .divide(ONE_HUNDRED, 0, RoundingMode.DOWN)
                    .longValueExact();
            allocated += base[i];
        }
        long remainder = totalCents - allocated;
        List<Long> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(base[i] + (i < remainder ? 1 : 0));
        }
        return result;
    }
}
