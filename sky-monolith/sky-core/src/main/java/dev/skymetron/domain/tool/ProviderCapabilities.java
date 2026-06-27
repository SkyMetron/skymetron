package dev.skymetron.domain.tool;

/**
 * Declares what a provider can do — context window, max tokens, streaming.
 */
public record ProviderCapabilities(
        int maxContextTokens,
        int maxOutputTokens,
        boolean supportsStreaming,
        boolean supportsSystemPrompt,
        boolean supportsTools
) {
    public static ProviderCapabilities standard(int maxContext, int maxOutput) {
        return new ProviderCapabilities(maxContext, maxOutput, true, true, false);
    }
}
