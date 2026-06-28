package dev.skymetron.application.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VaultBootstrapService tests")
class VaultBootstrapServiceTest {

    private VaultBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new VaultBootstrapService();
    }

    @Test
    @DisplayName("scan returns 0 for non-existent path")
    void scanNonExistentPath(@TempDir Path tempDir) throws IOException {
        Path missingPath = tempDir.resolve("does-not-exist");
        var result = service.scan(missingPath);

        assertThat(result.markdownFiles()).isZero();
        assertThat(result.totalFiles()).isZero();
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("does not exist");
    }

    @Test
    @DisplayName("scan counts markdown files correctly")
    void scanCountsMarkdownFiles(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("notes"));
        Files.writeString(tempDir.resolve("notes").resolve("file1.md"), "# Hello");
        Files.writeString(tempDir.resolve("notes").resolve("file2.md"), "# World");
        Files.writeString(tempDir.resolve("notes").resolve("readme.txt"), "plain text");
        Files.writeString(tempDir.resolve("config.json"), "{}");

        var result = service.scan(tempDir);

        assertThat(result.markdownFiles()).isEqualTo(2);
        assertThat(result.totalFiles()).isEqualTo(4);
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("scan counts nested markdown files")
    void scanNestedMarkdownFiles(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("deep").resolve("nested"));
        Files.writeString(tempDir.resolve("deep").resolve("nested").resolve("doc.md"), "# Doc");
        Files.writeString(tempDir.resolve("deep").resolve("readme.md"), "# Readme");

        var result = service.scan(tempDir);

        assertThat(result.markdownFiles()).isEqualTo(2);
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("scan returns total size in bytes")
    void scanTotalSize(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.md"), "Hello World");
        Files.writeString(tempDir.resolve("b.md"), "Foo Bar Baz");

        var result = service.scan(tempDir);

        assertThat(result.totalSizeBytes()).isPositive();
        assertThat(result.markdownFiles()).isEqualTo(2);
    }

    @Test
    @DisplayName("scan returns 0 for empty directory")
    void scanEmptyDirectory(@TempDir Path tempDir) throws IOException {
        var result = service.scan(tempDir);

        assertThat(result.markdownFiles()).isZero();
        assertThat(result.totalFiles()).isZero();
        assertThat(result.success()).isTrue();
    }
}
