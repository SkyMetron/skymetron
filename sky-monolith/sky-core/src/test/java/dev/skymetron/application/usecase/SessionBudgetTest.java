package dev.skymetron.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionBudget tests")
class SessionBudgetTest {

    private SessionBudget budget(int maxReq, int maxDay, int costCents) {
        return new SessionBudget(maxReq, maxDay, costCents, false, false, 80);
    }

    @Test
    @DisplayName("canSpend() returns true when within limits")
    void canSpendWithinLimits() {
        SessionBudget b = budget(4096, 100000, 0);
        assertThat(b.canSpend(2048)).isTrue();
    }

    @Test
    @DisplayName("canSpend() returns false when exceeding per-request limit")
    void canSpendExceedsPerRequest() {
        SessionBudget b = budget(4096, 100000, 0);
        assertThat(b.canSpend(8192)).isFalse();
    }

    @Test
    @DisplayName("canSpend() returns false when daily budget exhausted")
    void canSpendDailyExhausted() {
        SessionBudget b = budget(4096, 100, 0);
        b.recordUsage(80);
        assertThat(b.canSpend(50)).isFalse();
    }

    @Test
    @DisplayName("recordUsage() accumulates tokens")
    void recordUsageAccumulates() {
        SessionBudget b = budget(4096, 100000, 0);
        b.recordUsage(100);
        b.recordUsage(200);
        assertThat(b.tokensUsedToday()).isEqualTo(300);
    }

    @Test
    @DisplayName("isFreeOnly() returns true when maxCostCentsPerDay is 0")
    void isFreeOnly() {
        SessionBudget b = budget(4096, 100000, 0);
        assertThat(b.isFreeOnly()).isTrue();
    }

    @Test
    @DisplayName("isFreeOnly() returns false when cost > 0")
    void isNotFreeOnly() {
        SessionBudget b = budget(4096, 100000, 500);
        assertThat(b.isFreeOnly()).isFalse();
    }

    @Test
    @DisplayName("loopBudgetPercent() returns 20 when reserve is 80")
    void loopBudgetPercent() {
        SessionBudget b = budget(4096, 100000, 0);
        assertThat(b.loopBudgetPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("availableTokens() decreases with usage")
    void availableTokens() {
        SessionBudget b = budget(4096, 1000, 0);
        b.recordUsage(300);
        assertThat(b.availableTokens()).isEqualTo(700);
    }
}
