package dev.skymetron.infrastructure.ai.provider;

import java.util.List;

/**
 * OpenAI-compatible chat completion response (shared by Mistral, NVIDIA, Groq, OpenRouter).
 */
public record ChatCompletionResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(int index, Message message, String finish_reason) {}
    public record Message(String role, String content) {}
    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {
        public int promptTokens() { return prompt_tokens; }
        public int completionTokens() { return completion_tokens; }
    }
}
