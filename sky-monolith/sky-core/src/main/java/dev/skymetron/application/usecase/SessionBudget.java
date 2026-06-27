package dev.skymetron.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-session token and cost budget.
 *
 * <p>Enforces the free-only constraint (maxCostCentsPerDay = 0) and the
 * 80/20 rule: 80% of rate limit reserved for user, 20% for autonomous loops.
 */
@Component
public class SessionBudget {

    private final int maxTokensPerRequest;
    private final int maxTokensPerDay;
    private final int maxCostCentsPerDay;
    private final boolean allowBackgroundResearch;
    private final boolean allowAutonomousLoops;
    private final int reservePercentForUser;

    private int tokensUsedToday = 0;

    public SessionBudget(
            @Value("${sky.budget.max-tokens-per-request:4096}") int maxTokensPerRequest,
            @Value("${sky.budget.max-tokens-per-day:100000}") int maxTokensPerDay,
            @Value("${sky.budget.max-cost-cents-per-day:0}") int maxCostCentsPerDay,
            @Value("${sky.budget.allow-background-research:false}") boolean allowBackgroundResearch,
            @Value("${sky.budget.allow-autonomous-loops:false}") boolean allowAutonomousLoops,
            @Value("${sky.budget.reserve-percent-for-user:80}") int reservePercentForUser) {
        this.maxTokensPerRequest = maxTokensPerRequest;
        this.maxTokensPerDay = maxTokensPerDay;
        this.maxCostCentsPerDay = maxCostCentsPerDay;
        this.allowBackgroundResearch = allowBackgroundResearch;
        this.allowAutonomousLoops = allowAutonomousLoops;
        this.reservePercentForUser = reservePercentForUser;
    }

    public boolean canSpend(int tokens) {
        return tokens <= maxTokensPerRequest && (tokensUsedToday + tokens) <= maxTokensPerDay;
    }

    public void recordUsage(int tokens) {
        tokensUsedToday += tokens;
    }

    public boolean isFreeOnly() {
        return maxCostCentsPerDay == 0;
    }

    public int availableTokens() {
        return Math.max(0, maxTokensPerDay - tokensUsedToday);
    }

    public int loopBudgetPercent() {
        return 100 - reservePercentForUser;
    }

    public int maxTokensPerRequest() { return maxTokensPerRequest; }
    public int maxTokensPerDay() { return maxTokensPerDay; }
    public int maxCostCentsPerDay() { return maxCostCentsPerDay; }
    public boolean allowBackgroundResearch() { return allowBackgroundResearch; }
    public boolean allowAutonomousLoops() { return allowAutonomousLoops; }
    public int reservePercentForUser() { return reservePercentForUser; }
    public int tokensUsedToday() { return tokensUsedToday; }
}
