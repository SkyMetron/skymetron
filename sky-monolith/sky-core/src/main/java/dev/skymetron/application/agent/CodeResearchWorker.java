package dev.skymetron.application.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class CodeResearchWorker implements ResearchWorker {

    private static final Logger log = LoggerFactory.getLogger(CodeResearchWorker.class);

    private final WebSearchWorker webSearchWorker;

    public CodeResearchWorker(WebSearchWorker webSearchWorker) {
        this.webSearchWorker = webSearchWorker;
    }

    @Override
    public String type() { return "code"; }

    @Override
    public CompletableFuture<WorkerResult> execute(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[CodeResearchWorker] searching: '{}'", query);
            try {
                String codeQuery = "site:github.com OR site:stackoverflow.com OR site:docs.oracle.com " + query;
                String result = webSearchWorker.search(codeQuery);
                return WorkerResult.ok("code", query, result, List.of("code-search"));
            } catch (Exception e) {
                log.warn("[CodeResearchWorker] failed: {}", e.getMessage());
                return WorkerResult.failed("code", query, e.getMessage());
            }
        });
    }
}
