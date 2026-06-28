package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.bootstrap.WorkspaceBootstrapService;
import dev.skymetron.application.bootstrap.WorkspaceConfig;
import dev.skymetron.infrastructure.security.JwtTokenProvider;
import dev.skymetron.infrastructure.security.github.GitHubAuthService;
import dev.skymetron.infrastructure.security.github.GitHubOrg;
import dev.skymetron.infrastructure.security.github.GitHubUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication — GitHub OAuth only")
public class AuthController {

    private final GitHubAuthService gitHubAuthService;
    private final JwtTokenProvider tokenProvider;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public AuthController(GitHubAuthService gitHubAuthService,
                          JwtTokenProvider tokenProvider,
                          WorkspaceBootstrapService workspaceBootstrapService) {
        this.gitHubAuthService = gitHubAuthService;
        this.tokenProvider = tokenProvider;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Operation(summary = "Get GitHub OAuth URL", description = "Returns the GitHub OAuth authorization URL")
    @GetMapping("/github/url")
    public ResponseEntity<?> getGitHubUrl(@RequestParam(defaultValue = "http://localhost:8080/api/auth/github/callback") String redirectUri) {
        String url = gitHubAuthService.getAuthorizationUrl(redirectUri);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "GitHub OAuth Callback", description = "Exchange GitHub code for access token and return user info")
    @PostMapping("/github")
    public ResponseEntity<?> githubLogin(@Valid @RequestBody GitHubAuthRequest request) {
        try {
            var tokenResponse = gitHubAuthService.exchangeCode(request.code(), request.redirectUri());
            GitHubUser gitHubUser = gitHubAuthService.fetchUser(tokenResponse.accessToken());
            List<GitHubOrg> orgs = gitHubAuthService.fetchOrgs(tokenResponse.accessToken());

            boolean isOrgMember = gitHubAuthService.isOrgMember(orgs, "SkyMetron");
            boolean isMaintainer = isOrgMember && "Joao-Aschenbrenner".equals(gitHubUser.login());

            String userType = isMaintainer ? "DEVELOPER" : "USER";
            List<String> roles = isMaintainer
                ? List.of("ADMIN", "USER")
                : List.of("USER");

            String jwt = tokenProvider.createToken(gitHubUser.login(), roles);

            return ResponseEntity.ok(Map.of(
                "token", jwt,
                "username", gitHubUser.login(),
                "name", gitHubUser.name() != null ? gitHubUser.name() : gitHubUser.login(),
                "email", gitHubUser.email() != null ? gitHubUser.email() : "",
                "avatarUrl", gitHubUser.avatarUrl() != null ? gitHubUser.avatarUrl() : "",
                "userType", userType,
                "isOrgMember", isOrgMember,
                "roles", roles
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "GitHub authentication failed",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Current user", description = "Returns authenticated username from JWT token")
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute(required = false) String username) {
        return ResponseEntity.ok(Map.of("username", username != null ? username : ""));
    }

    public record GitHubAuthRequest(@NotBlank String code, @NotBlank String redirectUri) {}
}
