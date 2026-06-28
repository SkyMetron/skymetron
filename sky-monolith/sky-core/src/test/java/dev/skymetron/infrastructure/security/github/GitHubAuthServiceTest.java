package dev.skymetron.infrastructure.security.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitHubAuthService tests")
class GitHubAuthServiceTest {

    private GitHubAuthService service;

    @BeforeEach
    void setUp() {
        service = new GitHubAuthService("test-client-id", "test-client-secret");
    }

    @Test
    @DisplayName("getOAuthUrl contains expected parameters")
    void getOAuthUrl() {
        String url = service.getOAuthUrl("http://localhost:3000/callback");

        assertThat(url).startsWith("https://github.com/login/oauth/authorize");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fcallback");
        assertThat(url).contains("scope=read%3Auser+user%3Aemail+read%3Aorg");
        assertThat(url).contains("response_type=code");
    }

    @Test
    @DisplayName("getAuthorizationUrl delegates to getOAuthUrl")
    void getAuthorizationUrl() {
        String url = service.getAuthorizationUrl("http://localhost:3000/callback");
        assertThat(url).startsWith("https://github.com/login/oauth/authorize");
    }

    @Test
    @DisplayName("getOAuthUrl handles null redirectUri")
    void getOAuthUrlWithNullRedirect() {
        String url = service.getOAuthUrl(null);
        assertThat(url).contains("redirect_uri=");
    }

    @Test
    @DisplayName("isOrgMember returns true when org is in list")
    void isOrgMemberTrue() {
        var orgs = List.of(
            new GitHubOrg("SkyMetron", "The SkyMetron org"),
            new GitHubOrg("OtherOrg", "Another org")
        );
        assertThat(service.isOrgMember(orgs, "SkyMetron")).isTrue();
    }

    @Test
    @DisplayName("isOrgMember returns false when org is not in list")
    void isOrgMemberFalse() {
        var orgs = List.of(
            new GitHubOrg("OtherOrg", "Another org")
        );
        assertThat(service.isOrgMember(orgs, "SkyMetron")).isFalse();
    }

    @Test
    @DisplayName("isOrgMember is case-insensitive")
    void isOrgMemberCaseInsensitive() {
        var orgs = List.of(
            new GitHubOrg("skymetron", "The SkyMetron org")
        );
        assertThat(service.isOrgMember(orgs, "SkyMetron")).isTrue();
        assertThat(service.isOrgMember(orgs, "SKYMETRON")).isTrue();
    }

    @Test
    @DisplayName("isOrgMember returns false for empty org list")
    void isOrgMemberEmptyList() {
        assertThat(service.isOrgMember(List.of(), "SkyMetron")).isFalse();
    }

    @Test
    @DisplayName("getOAuthUrl encodes special characters in redirectUri")
    void getOAuthUrlEncodesSpecialChars() {
        String url = service.getOAuthUrl("http://example.com/auth?param=value&x=1");
        assertThat(url).contains("redirect_uri=http%3A%2F%2Fexample.com%2Fauth%3Fparam%3Dvalue%26x%3D1");
    }
}
