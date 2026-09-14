package com.ortakpay.core.split;

import com.ortakpay.core.domain.SplitType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SplitStrategyResolver {

    private final Map<SplitType, SplitStrategy> strategiesByType;

    public SplitStrategyResolver(List<SplitStrategy> strategies) {
        this.strategiesByType =
                strategies.stream().collect(Collectors.toUnmodifiableMap(SplitStrategy::supports, Function.identity()));
    }

    public SplitStrategy resolve(SplitType splitType) {
        SplitStrategy strategy = strategiesByType.get(splitType);
        if (strategy == null) {
            throw new IllegalStateException("No SplitStrategy registered for split type: " + splitType);
        }
        return strategy;
    }
}
