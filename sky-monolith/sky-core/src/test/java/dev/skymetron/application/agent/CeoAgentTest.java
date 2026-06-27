package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.SessionBudget;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CeoAgent tests")
class CeoAgentTest {

    @Mock
    ProviderRegistry providerRegistry;
    @Mock
    SessionBudget budget;
    @Mock
    MemoryAgent memoryAgent;
    @Mock
    ToolAgent toolAgent;
    @Mock
    ResearchAgent researchAgent;
    @Mock
    ResearchSwarmAgent researchSwarmAgent;
    @Mock
    KnowledgeAgent knowledgeAgent;
    @Mock
    QaAgent qaAgent;
    @Mock
    SecurityAgent securityAgent;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    CeoAgent ceoAgent;

    @BeforeEach
    void setUp() {
        lenient().when(budget.maxTokensPerRequest()).thenReturn(4096);
        ceoAgent = new CeoAgent(providerRegistry, budget, memoryAgent, toolAgent, researchAgent,
                researchSwarmAgent, knowledgeAgent, qaAgent, securityAgent, eventPublisher, metrics);
    }

    @Test
    @DisplayName("classifyIntent() detects MEMORY_QUERY for memory-related text")
    void classifyMemoryQuery() {
        Intent intent = ceoAgent.classifyIntent("o que voce sabe sobre mim?");
        assertThat(intent).isEqualTo(Intent.MEMORY_QUERY);
    }

    @Test
    @DisplayName("classifyIntent() detects MEMORY_STORE for save commands")
    void classifyMemoryStore() {
        Intent intent = ceoAgent.classifyIntent("lembre que eu gosto de Java");
        assertThat(intent).isEqualTo(Intent.MEMORY_STORE);
    }

    @Test
    @DisplayName("classifyIntent() detects TOOL_EXECUTION for file operations")
    void classifyToolExecution() {
        Intent intent = ceoAgent.classifyIntent("liste arquivos do diretorio");
        assertThat(intent).isEqualTo(Intent.TOOL_EXECUTION);
    }

    @Test
    @DisplayName("classifyIntent() detects CODE_HELP for code-related text")
    void classifyCodeHelp() {
        Intent intent = ceoAgent.classifyIntent("escreve uma funcao em Java");
        assertThat(intent).isEqualTo(Intent.CODE_HELP);
    }

    @Test
    @DisplayName("classifyIntent() detects RESEARCH for search text")
    void classifyResearch() {
        Intent intent = ceoAgent.classifyIntent("pesquise sobre Spring Boot 3");
        assertThat(intent).isEqualTo(Intent.RESEARCH);
    }

    @Test
    @DisplayName("classifyIntent() detects RESEARCH_SWARM for deep research")
    void classifyResearchSwarm() {
        Intent intent = ceoAgent.classifyIntent("pesquise profundamente sobre Spring Boot 3");
        assertThat(intent).isEqualTo(Intent.RESEARCH_SWARM);
    }

    @Test
    @DisplayName("classifyIntent() detects RESEARCH_SWARM for multi-source research")
    void classifyResearchSwarmMulti() {
        Intent intent = ceoAgent.classifyIntent("pesquisa completa sobre Java 21");
        assertThat(intent).isEqualTo(Intent.RESEARCH_SWARM);
    }

    @Test
    @DisplayName("classifyIntent() defaults to GENERAL_CHAT")
    void classifyGeneralChat() {
        Intent intent = ceoAgent.classifyIntent("ola, como vai?");
        assertThat(intent).isEqualTo(Intent.GENERAL_CHAT);
    }

    @Test
    @DisplayName("classifyIntent() detects SKILL_MANAGEMENT for skill commands")
    void classifySkillManagement() {
        assertThat(ceoAgent.classifyIntent("skill add|test|desc|cat|content")).isEqualTo(Intent.SKILL_MANAGEMENT);
        assertThat(ceoAgent.classifyIntent("knowledge list")).isEqualTo(Intent.SKILL_MANAGEMENT);
        assertThat(ceoAgent.classifyIntent("skill list all")).isEqualTo(Intent.SKILL_MANAGEMENT);
    }

    @Test
    @DisplayName("classifyIntent() detects QA_TEST for test commands")
    void classifyQaTest() {
        assertThat(ceoAgent.classifyIntent("qa|run|test1|target|code")).isEqualTo(Intent.QA_TEST);
        assertThat(ceoAgent.classifyIntent("test|run|test1|target|code")).isEqualTo(Intent.QA_TEST);
        assertThat(ceoAgent.classifyIntent("run|test1|target|code")).isEqualTo(Intent.QA_TEST);
    }

    @Test
    @DisplayName("classifyIntent() detects SECURITY_ANALYSIS for scan commands")
    void classifySecurityAnalysis() {
        assertThat(ceoAgent.classifyIntent("scan|target|code")).isEqualTo(Intent.SECURITY_ANALYSIS);
        assertThat(ceoAgent.classifyIntent("security scan this code")).isEqualTo(Intent.SECURITY_ANALYSIS);
        assertThat(ceoAgent.classifyIntent("analise de seguranca")).isEqualTo(Intent.SECURITY_ANALYSIS);
    }

    @Test
    @DisplayName("selectTaskType() routes CODE_HELP to CODE_GENERATION")
    void selectTaskTypeCode() {
        TaskType type = ceoAgent.selectTaskType(Intent.CODE_HELP, "escreve codigo");
        assertThat(type).isEqualTo(TaskType.CODE_GENERATION);
    }

    @Test
    @DisplayName("selectTaskType() routes RESEARCH to WEB_RESEARCH")
    void selectTaskTypeResearch() {
        TaskType type = ceoAgent.selectTaskType(Intent.RESEARCH, "pesquise");
        assertThat(type).isEqualTo(TaskType.WEB_RESEARCH);
    }

    @Test
    @DisplayName("selectTaskType() routes RESEARCH_SWARM to LONG_ANALYSIS")
    void selectTaskTypeSwarm() {
        TaskType type = ceoAgent.selectTaskType(Intent.RESEARCH_SWARM, "pesquise profundamente");
        assertThat(type).isEqualTo(TaskType.LONG_ANALYSIS);
    }

    @Test
    @DisplayName("process() returns successful response when provider works")
    void processSuccess() {
        LlmResponse llmResp = new LlmResponse("Hello!", "model", "mistral", 10, 5, Duration.ofMillis(200), false, false);
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(llmResp));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.GENERAL_CHAT, "ola");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("process() returns failure when provider throws")
    void processFailure() {
        when(providerRegistry.chat(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("all providers down")));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.GENERAL_CHAT, "test");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isFalse();
        assertThat(response.content()).contains("all providers down");
    }

    @Test
    @DisplayName("process() records fallback responses correctly")
    void processFallback() {
        LlmResponse llmResp = new LlmResponse("Fallback!", "model", "nvidia", 10, 5, Duration.ofMillis(300), false, true);
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(llmResp));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.GENERAL_CHAT, "test");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("Fallback!");
    }

    @Test
    @DisplayName("process() routes MEMORY_QUERY to MemoryAgent")
    void processRoutesToMemory() {
        AgentRequest memRequest = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_QUERY, "o que voce sabe");
        when(memoryAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(memRequest, "found memories", 0.9, Duration.ofMillis(50))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.MEMORY_QUERY, "o que voce sabe sobre mim?");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("found memories");
    }

    @Test
    @DisplayName("process() routes TOOL_EXECUTION to ToolAgent")
    void processRoutesToTool() {
        AgentRequest toolRequest = AgentRequest.simple(toolAgent.id(), Intent.TOOL_EXECUTION, "filesystem|list|/tmp");
        when(toolAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(toolRequest, "file1\nfile2", 0.9, Duration.ofMillis(30))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.TOOL_EXECUTION, "list files");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("file1\nfile2");
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(ceoAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("id() returns a stable CEO id")
    void idStable() {
        assertThat(ceoAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @DisplayName("capabilities() declares supported intents")
    void capabilitiesDeclaresIntents() {
        AgentCapabilities caps = ceoAgent.capabilities();
        assertThat(caps.supports(Intent.GENERAL_CHAT)).isTrue();
        assertThat(caps.supports(Intent.CODE_HELP)).isTrue();
        assertThat(caps.supports(Intent.SKILL_MANAGEMENT)).isTrue();
        assertThat(caps.supports(Intent.QA_TEST)).isTrue();
        assertThat(caps.supports(Intent.SECURITY_ANALYSIS)).isTrue();
        assertThat(caps.supports(Intent.RESEARCH_SWARM)).isTrue();
    }

    @Test
    @DisplayName("process() routes SKILL_MANAGEMENT to KnowledgeAgent")
    void processRoutesToKnowledge() {
        AgentRequest skillRequest = AgentRequest.simple(knowledgeAgent.id(), Intent.SKILL_MANAGEMENT, "list");
        when(knowledgeAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(skillRequest, "skills: Java, Python", 0.9, Duration.ofMillis(20))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.SKILL_MANAGEMENT, "skill list");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("skills");
    }

    @Test
    @DisplayName("process() routes RESEARCH_SWARM to ResearchSwarmAgent")
    void processRoutesToResearchSwarm() {
        AgentRequest swarmRequest = AgentRequest.simple(researchSwarmAgent.id(), Intent.RESEARCH_SWARM, "deep research");
        when(researchSwarmAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(swarmRequest, "Swarm complete: 3 workers", 0.85, Duration.ofMillis(500))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.RESEARCH_SWARM, "pesquise profundamente sobre Java 21");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Swarm complete");
    }

    @Test
    @DisplayName("process() routes QA_TEST to QaAgent")
    void processRoutesToQa() {
        AgentRequest qaRequest = AgentRequest.simple(qaAgent.id(), Intent.QA_TEST, "run|test1|src|code");
        when(qaAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(qaRequest, "Tests passed", 0.8, Duration.ofMillis(50))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.QA_TEST, "qa|run|test1|src|code");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Tests passed");
    }

    @Test
    @DisplayName("process() routes SECURITY_ANALYSIS to SecurityAgent")
    void processRoutesToSecurity() {
        AgentRequest secRequest = AgentRequest.simple(securityAgent.id(), Intent.SECURITY_ANALYSIS, "scan|app|code");
        when(securityAgent.process(any())).thenReturn(CompletableFuture.completedFuture(
                AgentResponse.success(secRequest, "Security scan: PASSED", 0.85, Duration.ofMillis(30))));

        AgentRequest request = AgentRequest.simple(ceoAgent.id(), Intent.SECURITY_ANALYSIS, "scan|app|code");
        AgentResponse response = ceoAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("PASSED");
    }
}
