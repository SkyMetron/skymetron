package dev.skymetron.infrastructure.security.github;

public record GitHubOrg(
    String login,
    String description
) {}
