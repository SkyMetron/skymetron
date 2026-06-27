package dev.skymetron.infrastructure.tool;

import dev.skymetron.application.usecase.AgentSafetyPolicy;
import dev.skymetron.domain.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web search tool using DuckDuckGo HTML endpoint (free, no API key required).
 *
 * <p>Returns extracted titles + snippets. Actions are evaluated as
 * NETWORK_EGRESS by SafetyPolicy.
 */
@Component
public class WebSearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final String DDG_URL = "https://html.duckduckgo.com/html/?q=";
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a[^>]*class=\"result__a\"[^>]*>(.*?)</a>.*?<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>",
            Pattern.DOTALL);

    private final AgentSafetyPolicy safetyPolicy;
    private final HttpClient httpClient;

    public WebSearchTool(AgentSafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String name() { return "websearch"; }

    @Override
    public String description() { return "Search the web via DuckDuckGo (free, no API key)."; }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.required("query", "string", "Search query"),
                ToolParameter.optional("maxResults", "int", "Max results (default 5, max 10)")
        );
    }

    @Override
    public ActionRisk defaultRisk() { return ActionRisk.NETWORK_EGRESS; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String query = String.valueOf(params.getOrDefault("query", ""));
        if (query.isBlank()) {
            return ToolResult.failure("Query is required", ActionRisk.READ_ONLY);
        }

        int maxResults = 5;
        Object maxParam = params.get("maxResults");
        if (maxParam != null) {
            try { maxResults = Math.min(Integer.parseInt(String.valueOf(maxParam)), 10); } catch (Exception ignored) {}
        }

        Action action = Action.network("web search: " + query, DDG_URL + URLEncoder.encode(query, StandardCharsets.UTF_8));
        PolicyDecision decision = safetyPolicy.evaluate(action);
        if (!decision.isAllowed()) {
            return ToolResult.denied(decision.reason());
        }

        try {
            String url = DDG_URL + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SkyMetron/0.1")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String results = parseResults(response.body(), maxResults);
            log.info("WebSearch: query='{}' returned {} chars", query, results.length());
            return ToolResult.success(results, ActionRisk.NETWORK_EGRESS);
        } catch (Exception e) {
            return ToolResult.failure("Search failed: " + e.getMessage(), ActionRisk.NETWORK_EGRESS);
        }
    }

    private String parseResults(String html, int maxResults) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = RESULT_PATTERN.matcher(html);
        int count = 0;
        while (matcher.find() && count < maxResults) {
            String title = stripTags(matcher.group(1)).strip();
            String snippet = stripTags(matcher.group(2)).strip();
            if (!title.isEmpty()) {
                count++;
                sb.append(count).append(". ").append(title).append("\n   ").append(snippet).append("\n\n");
            }
        }
        if (sb.isEmpty()) {
            return "No results found.";
        }
        return sb.toString();
    }

    private String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
    }
}
