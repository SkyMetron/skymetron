package dev.skymetron.domain.tool;

import java.util.List;
import java.util.Map;

/**
 * Request to an LLM provider.
 */
public record LlmRequest(
        String model,
        List<Message> messages,
        double temperature,
        int maxTokens,
        boolean stream,
        Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String model;
        private List<Message> messages;
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private boolean stream = false;
        private Map<String, Object> metadata = Map.of();

        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<Message> messages) { this.messages = messages; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder stream(boolean stream) { this.stream = stream; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public LlmRequest build() {
            return new LlmRequest(model, messages, temperature, maxTokens, stream, metadata);
        }
    }
}
