package com.ortakpay.core.split;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.dto.ParticipantInput;
import java.util.List;

public interface SplitStrategy {

    SplitType supports();

    /**
     * Preconditions: callers must have already verified that every participant
     * userId exists before calling this method.
     */
    List<ExpenseShare> calculateShares(Expense expense, List<ParticipantInput> participants);
}
