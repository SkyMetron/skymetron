package dev.skymetron.application.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class PaperResearchWorker implements ResearchWorker {

    private static final Logger log = LoggerFactory.getLogger(PaperResearchWorker.class);

    private final WebSearchWorker webSearchWorker;

    public PaperResearchWorker(WebSearchWorker webSearchWorker) {
        this.webSearchWorker = webSearchWorker;
    }

    @Override
    public String type() { return "paper"; }

    @Override
    public CompletableFuture<WorkerResult> execute(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[PaperResearchWorker] searching: '{}'", query);
            try {
                String paperQuery = "site:arxiv.org OR site:scholar.google.com OR site:researchgate.net " + query;
                String result = webSearchWorker.search(paperQuery);
                return WorkerResult.ok("paper", query, result, List.of("paper-search"));
            } catch (Exception e) {
                log.warn("[PaperResearchWorker] failed: {}", e.getMessage());
                return WorkerResult.failed("paper", query, e.getMessage());
            }
        });
    }
}
