package dev.skymetron.infrastructure.security.github;

import java.util.List;

public record GitHubUser(
    String login,
    String name,
    String email,
    String avatarUrl
) {}
