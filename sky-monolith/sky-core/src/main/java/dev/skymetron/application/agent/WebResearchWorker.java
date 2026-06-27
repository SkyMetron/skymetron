package dev.skymetron.application.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class WebResearchWorker implements ResearchWorker {

    private static final Logger log = LoggerFactory.getLogger(WebResearchWorker.class);

    private final WebSearchWorker webSearchWorker;

    public WebResearchWorker(WebSearchWorker webSearchWorker) {
        this.webSearchWorker = webSearchWorker;
    }

    @Override
    public String type() { return "web"; }

    @Override
    public CompletableFuture<WorkerResult> execute(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[WebResearchWorker] searching: '{}'", query);
            try {
                String result = webSearchWorker.search(query);
                return WorkerResult.ok("web", query, result, List.of("web-search"));
            } catch (Exception e) {
                log.warn("[WebResearchWorker] failed: {}", e.getMessage());
                return WorkerResult.failed("web", query, e.getMessage());
            }
        });
    }
}
