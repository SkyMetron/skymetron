package dev.skymetron.application.agent;

import java.util.List;

public record WorkerResult(
        String workerType,
        String query,
        String content,
        List<String> sources,
        boolean success,
        String error
) {
    public static WorkerResult ok(String workerType, String query, String content, List<String> sources) {
        return new WorkerResult(workerType, query, content, sources, true, null);
    }

    public static WorkerResult failed(String workerType, String query, String error) {
        return new WorkerResult(workerType, query, null, List.of(), false, error);
    }
}
