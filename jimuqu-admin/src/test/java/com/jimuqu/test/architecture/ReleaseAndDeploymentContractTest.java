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
        String fullStackRunner = Files.readString(repositoryFile("script/test-fullstack.mjs"));

        assertTrue(workflow.contains("image: mysql:8.4"));
        assertTrue(workflow.contains("image: redis:7.0-alpine"));
        assertTrue(workflow.contains("repository: chengliang4810/jimuqu-admin-ui"));
        assertTrue(workflow.contains("JIMU_TEST_FRONTEND_DIR: ${{ github.workspace }}/frontend"));
        assertTrue(workflow.contains("sudo apt-get install --yes default-mysql-client redis-tools"));
        assertTrue(workflow.contains("JIMU_PLAYWRIGHT_INSTALL_DEPS: \"true\""));
        assertTrue(workflow.contains("run: node script/test-fullstack.mjs"));
        assertTrue(fullStackRunner.contains("\"-DskipTests=false\""));
        assertTrue(fullStackRunner.contains("\"-DforkCount=0\""));
        assertTrue(fullStackRunner.contains("\"clean\""));
        assertTrue(fullStackRunner.contains("\"verify\""));
        assertTrue(fullStackRunner.contains("\"JIMU_PLAYWRIGHT_INSTALL_DEPS\""));
        assertTrue(fullStackRunner.contains("installPlaywrightDependencies"));
        assertTrue(workflow.contains("JIMU_SSE_HEARTBEAT_INTERVAL: 1000"));
        assertTrue(workflow.contains("JIMU_JUSTAUTH_ENABLED: true"));
        assertTrue(workflow.contains("JIMU_JUSTAUTH_GITEE_CLIENT_ID: http-contract-client"));
        assertTrue(workflow.contains("JIMU_JUSTAUTH_GITEE_CLIENT_SECRET: http-contract-secret"));
        assertTrue(workflow.contains("JIMU_JUSTAUTH_GITEE_REDIRECT_URI: http://127.0.0.1:15555/social-callback?source=gitee"));
        assertTrue(workflow.contains("JIMU_OSS_DOMAIN: http://127.0.0.1:15320/file/"));
        assertTrue(workflow.contains("JIMU_OSS_PATH: ./target/release-test-oss"));
        assertFalse(workflow.contains("mvn clean package -DskipTests"));
    }

    @Test
    void fullStackGateRunsCompletelyOnEverySupportedOperatingSystem() throws IOException {
        String workflow = Files.readString(repositoryFile(".github/workflows/release.yml"))
                .replace("\r\n", "\n");
        String fullStackRunner = Files.readString(repositoryFile("script/test-fullstack.mjs"));
        String datastorePreparation = Files.readString(repositoryFile("script/prepare-ci-datastores.mjs"));
        String lowerCaseWorkflow = workflow.toLowerCase(Locale.ROOT);

        assertTrue(workflow.contains("- ubuntu-latest"));
        assertTrue(workflow.contains("- macos-latest"));
        assertTrue(workflow.contains("- windows-latest"));
        assertTrue(workflow.contains("cross-platform-verify:"));
        assertTrue(workflow.contains("needs: cross-platform-verify"));
        assertTrue(workflow.contains("defaults:\n      run:\n        shell: bash"));
        assertTrue(workflow.contains("uses: ankane/setup-mysql@19fbb7ee54446ac7f3aed34db192ec70dbec6092"));
        assertTrue(workflow.contains("uses: shogo82148/actions-setup-redis@abd15d4028c04b9a6ea7917f1f3d931c14b6871f"));
        assertTrue(workflow.contains("redis-version: \"7.0\""));
        assertTrue(workflow.contains("choco install memurai-developer --version=4.1.8 --yes --no-progress --accept-license"));
        assertFalse(workflow.contains("uses: ankane/setup-mysql@v1"));
        assertFalse(workflow.contains("uses: shogo82148/actions-setup-redis@v1"));
        assertTrue(workflow.contains("node script/prepare-ci-datastores.mjs"));
        assertTrue(workflow.contains("node --check script/test-fullstack.mjs"));
        assertTrue(workflow.contains("name: Verify complete full stack"));
        assertTrue(workflow.contains("run: node script/test-fullstack.mjs"));
        assertFalse(workflow.contains("--preflight-only"));
        assertFalse(lowerCaseWorkflow.contains("shell: pwsh"));
        assertFalse(lowerCaseWorkflow.contains("powershell"));
        assertFalse(lowerCaseWorkflow.contains(".ps1"));
        assertFalse(workflow.contains("P@" + "ssw0rd"));
        assertTrue(workflow.contains("ci-${{ github.run_id }}-${{ github.run_attempt }}"));
        assertTrue(datastorePreparation.contains("process.platform === \"win32\""));
        assertTrue(datastorePreparation.contains("memurai-cli.exe"));
        assertTrue(datastorePreparation.contains("ALTER USER USER()"));
        assertFalse(datastorePreparation.contains("@'localhost'"));
        assertTrue(datastorePreparation.contains("GITHUB_ACTIONS"));

        assertTrue(fullStackRunner.contains("[mysql, [\"--version\"]]"));
        assertTrue(fullStackRunner.contains("[redisCli, [\"--version\"]]"));
        assertTrue(fullStackRunner.contains("[maven, [\"--version\"]]"));
        assertTrue(fullStackRunner.contains("[java, [\"--version\"]]"));
        assertTrue(fullStackRunner.contains("[corepack, [\"--version\"]]"));
        assertTrue(fullStackRunner.contains("\"memurai-cli.exe\""));
        assertFalse(fullStackRunner.contains("windowsShellChildren"));
        assertTrue(fullStackRunner.contains("\"taskkill.exe\""));
        assertTrue(fullStackRunner.contains("[\"/PID\", String(child.pid), \"/T\", \"/F\"]"));
        assertTrue(fullStackRunner.contains("timeout: 10_000"));
        assertTrue(fullStackRunner.contains("for (let attempt = 1; attempt <= 3; attempt++)"));
        assertTrue(fullStackRunner.contains("child.kill(\"SIGKILL\")"));
        assertTrue(fullStackRunner.contains("serviceLogCloseTimeoutMs = 10_000"));
        assertTrue(fullStackRunner.contains("service.child.stdout.unpipe(service.stdout)"));
        assertTrue(fullStackRunner.contains("\"node_modules\",\n" +
                "    \"vite\",\n" +
                "    \"bin\",\n" +
                "    \"vite.js\""));
        assertTrue(fullStackRunner.contains("process.execPath,\n" +
                "    [\n" +
                "      viteCli,"));
        assertFalse(fullStackRunner.contains("async function scanRedisKeys"));
        assertFalse(fullStackRunner.contains("FLUSHDB"));
        assertFalse(fullStackRunner.contains("FLUSHALL"));
        assertFalse(fullStackRunner.contains("under ${redisPrefix}"));
        assertFalse(fullStackRunner.contains(": ${redisPrefix}`"));

        int scanStart = fullStackRunner.indexOf("async function scanOwnedRedisKeys");
        int scanEnd = fullStackRunner.indexOf("async function removeOwnedRedisKeys");
        assertTrue(scanStart >= 0 && scanEnd > scanStart);
        String scanBlock = fullStackRunner.substring(scanStart, scanEnd);
        assertTrue(scanBlock.contains("`${redisPrefix}*`"));
        assertFalse(scanBlock.contains("logPath"));
    }

    @Test
    void releasePublishesFromMainOnlyWithoutContainerImages() throws IOException {
        String workflow = Files.readString(repositoryFile(".github/workflows/release.yml"))
                .replace("\r\n", "\n");
        String lowerCaseWorkflow = workflow.toLowerCase(Locale.ROOT);

        assertTrue(workflow.contains("push:\n    branches:\n      - main\n"));
        assertFalse(workflow.contains("\n      - dev\n"));
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
