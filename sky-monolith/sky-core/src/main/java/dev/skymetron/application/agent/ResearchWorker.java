package dev.skymetron.application.agent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ResearchWorker {
    String type();
    CompletableFuture<WorkerResult> execute(String query);
}
