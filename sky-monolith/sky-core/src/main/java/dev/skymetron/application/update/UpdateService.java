package dev.skymetron.application.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final String GITHUB_API = "https://api.github.com/repos/SkyMetron/skymetron/releases/latest";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UpdateService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public Optional<ReleaseInfo> checkLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("GitHub API returned {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String tagName = root.get("tag_name").asText();
            String name = root.get("name").asText();
            String publishedAt = root.get("published_at").asText();
            String body = root.get("body").asText();
            boolean prerelease = root.get("prerelease").asBoolean();

            return Optional.of(new ReleaseInfo(tagName, name, publishedAt, body, prerelease));
        } catch (Exception e) {
            log.warn("Failed to check for updates: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record ReleaseInfo(
        String tagName,
        String name,
        String publishedAt,
        String body,
        boolean prerelease
    ) {}
}
