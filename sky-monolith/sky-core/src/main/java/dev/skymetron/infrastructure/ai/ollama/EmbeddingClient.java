package dev.skymetron.infrastructure.ai.ollama;

import com.pgvector.PGvector;
import dev.skymetron.application.port.out.EmbeddingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter that calls sky-ai-services (Python/FastAPI) for embeddings.
 *
 * <p>The Python service forwards to Ollama {@code nomic-embed-text} locally.
 * This client is intentionally thin — no retry/circuit-breaker yet (Sprint 3+).
 */
@Component
@Lazy
public class EmbeddingClient implements EmbeddingPort {

    private final RestClient restClient;
    private final int dimensions;

    public EmbeddingClient(
            RestClient.Builder restClientBuilder,
            @Value("${sky.ai-services.base-url:http://localhost:8001}") String baseUrl,
            @Value("${sky.ollama.embedding-dimensions:768}") int dimensions) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        EmbeddingResponse response = restClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(EmbeddingResponse.class);
        if (response == null || response.embedding() == null) {
            throw new IllegalStateException("Empty embedding response from AI services for text: "
                    + truncate(text, 80));
        }
        return toFloatArray(response.embedding());
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        BatchEmbeddingResponse response = restClient.post()
                .uri("/api/embeddings/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("texts", texts))
                .retrieve()
                .body(BatchEmbeddingResponse.class);
        if (response == null || response.embeddings() == null) {
            throw new IllegalStateException("Empty batch embedding response");
        }
        List<float[]> result = new ArrayList<>(response.embeddings().size());
        for (List<Double> vec : response.embeddings()) {
            result.add(toFloatArray(vec));
        }
        return result;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private static float[] toFloatArray(List<Double> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).floatValue();
        }
        return arr;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * Converts a float array to pgvector literal string for native queries.
     */
    public static String toVectorLiteral(float[] embedding) {
        return new PGvector(embedding).toString();
    }

    record EmbeddingResponse(List<Double> embedding, String model, int dimensions) {
    }

    record BatchEmbeddingResponse(List<List<Double>> embeddings, String model, int dimensions) {
    }
}
