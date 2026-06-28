package dev.skymetron.infrastructure.security.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GitHubAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthService.class);
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String ORGS_URL = "https://api.github.com/user/orgs";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;

    public GitHubAuthService(
            @Value("${sky.github.client-id:}") String clientId,
            @Value("${sky.github.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getOAuthUrl(String redirectUri) {
        return "https://github.com/login/oauth/authorize"
            + "?client_id=" + urlEncode(clientId)
            + "&redirect_uri=" + urlEncode(redirectUri)
            + "&scope=" + urlEncode("read:user user:email read:org")
            + "&response_type=code";
    }

    public GitHubAccessToken exchangeCode(String code, String redirectUri) throws Exception {
        String body = "client_id=" + urlEncode(clientId)
            + "&client_secret=" + urlEncode(clientSecret)
            + "&code=" + urlEncode(code)
            + "&redirect_uri=" + urlEncode(redirectUri);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GitHub token exchange failed: " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        if (json.has("error")) {
            throw new RuntimeException("GitHub OAuth error: " + json.get("error").asText()
                + " - " + json.get("error_description").asText());
        }

        String accessToken = json.get("access_token").asText();
        String tokenType = json.get("token_type").asText();
        String scope = json.get("scope").asText();

        return new GitHubAccessToken(accessToken, tokenType, scope);
    }

    public GitHubUser fetchUser(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(USER_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/vnd.github.v3+json")
            .timeout(TIMEOUT)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GitHub user fetch failed: " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return new GitHubUser(
            json.get("login").asText(),
            json.has("name") && !json.get("name").isNull() ? json.get("name").asText() : null,
            json.has("email") && !json.get("email").isNull() ? json.get("email").asText() : null,
            json.has("avatar_url") && !json.get("avatar_url").isNull() ? json.get("avatar_url").asText() : null
        );
    }

    public List<GitHubOrg> fetchOrgs(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ORGS_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/vnd.github.v3+json")
            .timeout(TIMEOUT)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GitHub orgs fetch failed: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<GitHubOrg>>() {});
    }

    public boolean isOrgMember(List<GitHubOrg> orgs, String orgName) {
        return orgs.stream().anyMatch(org -> org.login().equalsIgnoreCase(orgName));
    }

    public String getAuthorizationUrl(String redirectUri) {
        return getOAuthUrl(redirectUri);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    public record GitHubAccessToken(String accessToken, String tokenType, String scope) {}
}
