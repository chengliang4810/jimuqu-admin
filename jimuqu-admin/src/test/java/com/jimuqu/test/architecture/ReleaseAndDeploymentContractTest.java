package com.jimuqu.test.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseAndDeploymentContractTest {

    @Test
    void releaseRunsFullVerificationAgainstMysqlAndRedis() throws IOException {
        String workflow = Files.readString(repositoryFile(".github/workflows/release.yml"));

        assertTrue(workflow.contains("image: mysql:8.4"));
        assertTrue(workflow.contains("image: redis:7.4-alpine"));
        assertTrue(workflow.contains("mvn --batch-mode clean verify -DskipTests=false -DforkCount=0"));
        assertFalse(workflow.contains("mvn clean package -DskipTests"));
    }

    @Test
    void releasePublishesFromDevOnlyWithoutContainerImages() throws IOException {
        String workflow = Files.readString(repositoryFile(".github/workflows/release.yml"))
                .replace("\r\n", "\n");
        String lowerCaseWorkflow = workflow.toLowerCase(Locale.ROOT);

        assertTrue(workflow.contains("push:\n    branches:\n      - dev\n"));
        assertFalse(workflow.contains("\n      - main\n"));
        assertFalse(lowerCaseWorkflow.contains("docker"));
        assertFalse(lowerCaseWorkflow.contains("ghcr"));
    }

    @Test
    void localOssAndWebsocketSettingsAreDeploymentPortable() throws IOException {
        String config = Files.readString(repositoryFile("jimuqu-admin/src/main/resources/app.yml"));

        assertTrue(config.contains("${JIMU_OSS_DOMAIN:/file/}"));
        assertTrue(config.contains("${JIMU_OSS_PATH:./data/oss/}"));
        assertTrue(config.contains("${JIMU_WEBSOCKET_HEARTBEAT_INTERVAL:60000}"));
        assertFalse(config.contains("domain: http://127.0.0.1:5320/file/"));
        assertFalse(config.contains("storage-path: D:/temp/"));
    }

    private Path repositoryFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到仓库文件: " + relativePath);
    }
}
