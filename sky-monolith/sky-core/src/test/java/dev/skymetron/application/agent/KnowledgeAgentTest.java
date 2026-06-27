package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeAgent tests")
class KnowledgeAgentTest {

    @Mock
    SkyMetricsRegistry metrics;

    KnowledgeAgent knowledgeAgent;

    @BeforeEach
    void setUp() {
        knowledgeAgent = new KnowledgeAgent(metrics);
    }

    @Test
    @DisplayName("handleSkillRequest() returns empty catalog message")
    void listEmpty() {
        String result = knowledgeAgent.handleSkillRequest("list");
        assertThat(result).contains("No skills");
    }

    @Test
    @DisplayName("handleSkillRequest() adds a skill")
    void addSkill() {
        String result = knowledgeAgent.handleSkillRequest("add|Java|Java programming language|programming|Java is a language");
        assertThat(result).contains("added");
        assertThat(knowledgeAgent.skillCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("handleSkillRequest() retrieves a skill")
    void getSkill() {
        knowledgeAgent.handleSkillRequest("add|Java|Java language|programming|Java content");
        String result = knowledgeAgent.handleSkillRequest("get|Java");
        assertThat(result).contains("Java");
        assertThat(result).contains("v1");
    }

    @Test
    @DisplayName("handleSkillRequest() updates a skill")
    void updateSkill() {
        knowledgeAgent.handleSkillRequest("add|Java|Java language|programming|v1 content");
        String result = knowledgeAgent.handleSkillRequest("update|Java|v2 content");
        assertThat(result).contains("v2");
    }

    @Test
    @DisplayName("handleSkillRequest() sets confidence")
    void setConfidence() {
        knowledgeAgent.handleSkillRequest("add|Java|Java lang|programming|content");
        String result = knowledgeAgent.handleSkillRequest("confidence|Java|0.95");
        assertThat(result).contains("0.95");
    }

    @Test
    @DisplayName("handleSkillRequest() deletes a skill")
    void deleteSkill() {
        knowledgeAgent.handleSkillRequest("add|Java|desc|prog|content");
        knowledgeAgent.handleSkillRequest("delete|Java");
        assertThat(knowledgeAgent.skillCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("id() returns stable id")
    void idStable() {
        assertThat(knowledgeAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000007"));
    }

    @Test
    @DisplayName("capabilities() includes SKILL_MANAGEMENT")
    void capabilities() {
        assertThat(knowledgeAgent.capabilities().supports(Intent.SKILL_MANAGEMENT)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(knowledgeAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
