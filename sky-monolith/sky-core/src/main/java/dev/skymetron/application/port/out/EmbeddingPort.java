package dev.skymetron.application.port.out;

import java.util.List;

/**
 * Outbound port for generating text embeddings.
 *
 * <p>Implemented by the infrastructure adapter that calls sky-ai-services
 * (Python/FastAPI), which in turn calls Ollama {@code nomic-embed-text}.
 */
public interface EmbeddingPort {

    /**
     * Generate an embedding vector for a single text.
     *
     * @return float array of 768 dimensions
     */
    float[] embed(String text);

    /**
     * Generate embeddings for a batch of texts (sequential to respect Ollama).
     *
     * @return list of float arrays, one per input text
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Number of dimensions produced by this embedding model.
     */
    int dimensions();
}
