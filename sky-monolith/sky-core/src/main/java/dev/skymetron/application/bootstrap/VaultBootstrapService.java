package dev.skymetron.application.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public class VaultBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(VaultBootstrapService.class);

    public VaultBootstrapResult scan(Path vaultPath) throws IOException {
        if (!Files.exists(vaultPath)) {
            return new VaultBootstrapResult(0, 0, 0, false, "Vault path does not exist");
        }

        long mdFiles;
        long totalFiles;
        long totalSizeBytes;

        try (Stream<Path> walk = Files.walk(vaultPath)) {
            mdFiles = walk.parallel()
                .filter(p -> p.toString().endsWith(".md"))
                .count();
        }

        try (Stream<Path> walk = Files.walk(vaultPath)) {
            totalFiles = walk.parallel()
                .filter(Files::isRegularFile)
                .count();
        }

        try (Stream<Path> walk = Files.walk(vaultPath)) {
            totalSizeBytes = walk.parallel()
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); }
                    catch (IOException e) { return 0; }
                })
                .sum();
        }

        log.info("Vault scan complete: {} files, {} markdown, {} bytes",
            totalFiles, mdFiles, totalSizeBytes);

        return new VaultBootstrapResult(mdFiles, totalFiles, totalSizeBytes, true, null);
    }

    public record VaultBootstrapResult(
        long markdownFiles,
        long totalFiles,
        long totalSizeBytes,
        boolean success,
        String error
    ) {}
}
