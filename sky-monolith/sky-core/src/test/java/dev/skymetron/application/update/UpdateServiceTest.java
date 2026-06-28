package dev.skymetron.application.update;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateService tests")
class UpdateServiceTest {

    @Test
    @DisplayName("checkLatestRelease handles response (offline or actual check)")
    void checkLatestRelease() {
        UpdateService service = new UpdateService();
        var result = service.checkLatestRelease();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("ReleaseInfo record stores all fields")
    void releaseInfoRecord() {
        var info = new UpdateService.ReleaseInfo("v0.2.0", "Release 0.2.0",
            "2026-06-28T12:00:00Z", "Release notes", false);

        assertThat(info.tagName()).isEqualTo("v0.2.0");
        assertThat(info.name()).isEqualTo("Release 0.2.0");
        assertThat(info.publishedAt()).isEqualTo("2026-06-28T12:00:00Z");
        assertThat(info.body()).isEqualTo("Release notes");
        assertThat(info.prerelease()).isFalse();
    }

    @Test
    @DisplayName("ReleaseInfo can be a prerelease")
    void releaseInfoPrerelease() {
        var info = new UpdateService.ReleaseInfo("v0.3.0-rc1", "Release Candidate 1",
            "2026-07-01T00:00:00Z", "RC notes", true);

        assertThat(info.prerelease()).isTrue();
        assertThat(info.tagName()).isEqualTo("v0.3.0-rc1");
    }
}
