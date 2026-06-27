package dev.skymetron.application.agent;

import dev.skymetron.domain.tool.ToolResult;
import dev.skymetron.infrastructure.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Web Research Worker — performs a single web search via {@link WebSearchTool}.
 *
 * <p>Workers are lightweight, stateless, and created/destroyed per research mission.
 * Sprint 5 has only this worker type; Sprint 10 adds Docs/Code/Paper workers
 * via the ResearchSwarmAgent.
 */
@Component
public class WebSearchWorker {

    private static final Logger log = LoggerFactory.getLogger(WebSearchWorker.class);

    private final WebSearchTool searchTool;

    public WebSearchWorker(WebSearchTool searchTool) {
        this.searchTool = searchTool;
    }

    /**
     * Execute a web search for the given query.
     *
     * @return raw search results (titles + snippets)
     */
    public String search(String query) {
        log.info("[WebSearchWorker] searching: '{}'", query);
        ToolResult result = searchTool.execute(Map.of("query", query, "maxResults", 5));

        if (result.success()) {
            return result.output();
        } else {
            log.warn("[WebSearchWorker] search failed: {}", result.error());
            return "Search failed: " + result.error();
        }
    }
}
