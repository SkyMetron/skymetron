package dev.skymetron.domain.tool;

/**
 * A single message in an LLM conversation.
 */
public record Message(
        Role role,
        String content
) {
    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }
}
