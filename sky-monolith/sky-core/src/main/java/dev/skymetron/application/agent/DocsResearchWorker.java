package dev.skymetron.application.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class DocsResearchWorker implements ResearchWorker {

    private static final Logger log = LoggerFactory.getLogger(DocsResearchWorker.class);

    private final WebSearchWorker webSearchWorker;

    public DocsResearchWorker(WebSearchWorker webSearchWorker) {
        this.webSearchWorker = webSearchWorker;
    }

    @Override
    public String type() { return "docs"; }

    @Override
    public CompletableFuture<WorkerResult> execute(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[DocsResearchWorker] searching: '{}'", query);
            try {
                String docsQuery = "site:docs.oracle.com OR site:docs.spring.io OR site:docs.docker.com OR site:kubernetes.io/docs " + query;
                String result = webSearchWorker.search(docsQuery);
                return WorkerResult.ok("docs", query, result, List.of("documentation-search"));
            } catch (Exception e) {
                log.warn("[DocsResearchWorker] failed: {}", e.getMessage());
                return WorkerResult.failed("docs", query, e.getMessage());
            }
        });
    }
}
