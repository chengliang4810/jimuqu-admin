#!/usr/bin/env node

import { spawn, spawnSync } from "node:child_process";
import {
  accessSync,
  constants as fsConstants,
  createWriteStream,
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
} from "node:fs";
import { access, mkdir, readFile, rm } from "node:fs/promises";
import net from "node:net";
import os from "node:os";
import path from "node:path";
import process from "node:process";
import { finished } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import { setTimeout as delay } from "node:timers/promises";
import { inflateRawSync } from "node:zlib";

const isWindows = process.platform === "win32";
const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const backendPort = portEnvironment("JIMU_TEST_BACKEND_PORT", 15320);
const frontendPort = portEnvironment("JIMU_TEST_FRONTEND_PORT", 15555);
const redisDatabase = 15;
const expectedHttpOperationCount = 149;
const serviceLogCloseTimeoutMs = 10_000;
/**
 * Redis 清理等待上限，覆盖测试 JVM 退出时仍在收尾的异步写入。
 */
const redisCleanupTimeoutMs = 20_000;
/**
 * Redis 连续空扫描次数，只有达到该次数才判定测试前缀已稳定清空。
 */
const redisCleanupStableChecks = 5;
/**
 * Redis 稳定性扫描间隔，避免紧密轮询占用本地服务资源。
 */
const redisCleanupPollIntervalMs = 500;
/**
 * 完整门禁中 Maven 主 JVM 的保守默认内存，避免宿主机大内存推导出过高堆预留。
 */
const defaultMavenOptions = "-Xms128m -Xmx1536m -XX:+UseSerialGC";
const startupTimeoutSeconds = Number(
  process.env.JIMU_TEST_STARTUP_TIMEOUT_SECONDS ?? 180,
);
const preflightOnly = process.argv.includes("--preflight-only");
const installPlaywrightDependencies = booleanEnvironment(
  "JIMU_PLAYWRIGHT_INSTALL_DEPS",
  false,
);
const unknownArguments = process.argv
  .slice(2)
  .filter((arg) => arg !== "--preflight-only");

if (unknownArguments.length > 0) {
  throw new Error(`Unsupported argument(s): ${unknownArguments.join(", ")}`);
}
if (!Number.isInteger(startupTimeoutSeconds) || startupTimeoutSeconds <= 0) {
  throw new Error(
    "JIMU_TEST_STARTUP_TIMEOUT_SECONDS must be a positive integer.",
  );
}
if (backendPort === frontendPort) {
  throw new Error("Backend and frontend test ports must be different.");
}

const runStartedAt = new Date();
const timestamp = runStartedAt.toISOString().replace(/\D/g, "").slice(0, 17);
const randomSuffix = Math.random().toString(16).slice(2, 10);
const runId = `${timestamp}_${process.pid}_${randomSuffix}`;
const databaseName = `jimuqu_it_${runId}`;
const redisPrefix = `jimu:it:${runId}:`;
const artifactRoot = path.join(repoRoot, "runtime", "test", runId);
const ossPath = path.join(artifactRoot, "oss");
const jarPath = path.join(
  repoRoot,
  "jimuqu-admin",
  "target",
  "jimuqu-admin.jar",
);

let frontendRoot;
let mysql;
let redisCli;
let maven;
let java;
let corepack;
let backendEnvironment;
let frontendEnvironment;
let mysqlEnvironment;
let redisEnvironment;
let mysqlArguments;
let redisReady = false;
let databaseOwned = false;
let ossCreated = false;
let backendService;
let frontendService;
let interrupted;
const activeChildren = new Set();

function requiredEnvironment(name) {
  const value = process.env[name];
  if (!value?.trim()) {
    throw new Error(`Required environment variable is missing: ${name}`);
  }
  return value;
}

function environmentOrDefault(name, fallback) {
  const value = process.env[name];
  return value?.trim() ? value : fallback;
}

function booleanEnvironment(name, fallback) {
  const value = process.env[name]?.trim().toLowerCase();
  if (!value) {
    return fallback;
  }
  if (value === "true") {
    return true;
  }
  if (value === "false") {
    return false;
  }
  throw new Error(`${name} must be true or false.`);
}

/**
 * 读取可选测试端口，并拒绝无效或越界值。
 *
 * @param {string} name 环境变量名称
 * @param {number} fallback 默认端口
 * @returns {number} 校验后的端口
 */
function portEnvironment(name, fallback) {
  const value = process.env[name]?.trim();
  const port = Number(value || fallback);
  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error(`${name} must be an integer between 1 and 65535.`);
  }
  return port;
}

function isFile(filePath) {
  try {
    return statSync(filePath).isFile();
  } catch {
    return false;
  }
}

function isExecutableFile(filePath) {
  if (!isFile(filePath)) {
    return false;
  }
  if (isWindows) {
    return true;
  }
  try {
    accessSync(filePath, fsConstants.X_OK);
    return true;
  } catch {
    return false;
  }
}

function isDirectory(directoryPath) {
  try {
    return statSync(directoryPath).isDirectory();
  } catch {
    return false;
  }
}

function resolveTool(names, candidates = []) {
  const pathNames = isWindows
    ? names.flatMap((name) =>
        path.extname(name)
          ? [name]
          : [name, `${name}.cmd`, `${name}.exe`, `${name}.bat`],
      )
    : names;
  const searchDirectories = (process.env.PATH ?? "")
    .split(path.delimiter)
    .map((entry) => entry.replace(/^"(.*)"$/, "$1"))
    .filter(Boolean);

  for (const candidate of [
    ...candidates,
    ...searchDirectories.flatMap((directory) =>
      pathNames.map((name) => path.join(directory, name)),
    ),
  ]) {
    if (isExecutableFile(candidate)) {
      return path.resolve(candidate);
    }
  }
  throw new Error(`Required tool was not found: ${names[0]}`);
}

function resolveFrontendRoot() {
  const configured = process.env.JIMU_TEST_FRONTEND_DIR;
  if (configured?.trim()) {
    const resolved = path.resolve(configured);
    if (!isDirectory(resolved)) {
      throw new Error(`Frontend repository was not found: ${resolved}`);
    }
    return resolved;
  }

  const candidates = [
    path.resolve(repoRoot, "..", "jimuqu-admin-ui"),
    path.resolve(repoRoot, "..", "..", "WebProjects", "jimuqu-admin-ui"),
    path.join(path.parse(repoRoot).root, "WebProjects", "jimuqu-admin-ui"),
    path.resolve(process.cwd(), "jimuqu-admin-ui"),
  ];
  const match = candidates.find((candidate) =>
    isFile(path.join(candidate, "package.json")),
  );
  if (!match) {
    throw new Error(
      `Frontend repository was not found. Set JIMU_TEST_FRONTEND_DIR. Checked: ${[...new Set(candidates)].join(", ")}`,
    );
  }
  return match;
}

async function assertPortAvailable(port) {
  await new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();
    server.once("error", () => {
      reject(
        new Error(
          `Port ${port} is already in use. The test runner never reuses an existing service.`,
        ),
      );
    });
    server.listen({ host: "127.0.0.1", port, exclusive: true }, () => {
      server.close(resolve);
    });
  });
}

function assertPreflightConfiguration() {
  const baseConfigPath = path.join(
    repoRoot,
    "jimuqu-admin",
    "src",
    "main",
    "resources",
    "app.yml",
  );
  const testConfigPath = path.join(
    repoRoot,
    "jimuqu-admin",
    "src",
    "main",
    "resources",
    "app-test.yml",
  );
  const commonCacheConfigPath = path.join(
    repoRoot,
    "jimuqu-common",
    "jimuqu-common-cache",
    "src",
    "main",
    "resources",
    "config",
    "common-cache.yml",
  );
  const baseConfig = readFileSync(baseConfigPath, "utf8");
  const testConfig = readFileSync(testConfigPath, "utf8");
  const commonCacheConfig = readFileSync(commonCacheConfigPath, "utf8");
  for (const marker of [
    "${JIMU_REDIS_SERVER:127.0.0.1:6379}",
    "${JIMU_REDIS_DB:0}",
    "${JIMU_REDIS_PASSWORD:}",
    "${JIMU_REDIS_PREFIX:jimuqu}",
    "${JIMU_SSE_HEARTBEAT_INTERVAL:60000}",
  ]) {
    if (!baseConfig.includes(marker)) {
      throw new Error(
        `Base configuration is missing isolation marker ${marker}.`,
      );
    }
  }
  if (/keyHeader:/i.test(commonCacheConfig)) {
    throw new Error(
      `Common cache configuration must not override the application Redis prefix: ${commonCacheConfigPath}`,
    );
  }
  for (const marker of [
    "${JIMU_TEST_SERVER_PORT}",
    "${JIMU_TEST_MYSQL_DATABASE}",
    "${JIMU_TEST_REDIS_DB}",
    "${JIMU_TEST_REDIS_PASSWORD:}",
    "${JIMU_TEST_REDIS_PREFIX}",
    "${JIMU_TEST_OSS_PATH}",
    "${JIMU_TEST_SCHEDULED_JOB_PROBE_ENABLED:false}",
  ]) {
    if (!testConfig.includes(marker)) {
      throw new Error(
        `Test configuration is missing isolation marker ${marker}.`,
      );
    }
  }

  for (const relativePath of [
    "package.json",
    "pnpm-lock.yaml",
    "playwright.config.ts",
    path.join("tests", "e2e", "smoke.spec.ts"),
  ]) {
    const filePath = path.join(frontendRoot, relativePath);
    if (!isFile(filePath)) {
      throw new Error(`Frontend test prerequisite is missing: ${filePath}`);
    }
  }
  const frontendPackage = JSON.parse(
    readFileSync(path.join(frontendRoot, "package.json"), "utf8"),
  );
  if (frontendPackage.name !== "jimuqu-admin-ui") {
    throw new Error(
      `Frontend is not the Bell-Plus tree: ${frontendPackage.name ?? "<unnamed>"}`,
    );
  }
  if (frontendPackage.packageManager !== "pnpm@11.2.2") {
    throw new Error(
      `Frontend must pin Corepack to pnpm@11.2.2, actual: ${frontendPackage.packageManager ?? "<missing>"}`,
    );
  }
}

function listFilesRecursively(
  root,
  {
    excludeDirectories = new Set([
      ".git",
      ".idea",
      "node_modules",
      "runtime",
      "target",
    ]),
    include = () => true,
  } = {},
) {
  const files = [];
  const pending = [root];
  while (pending.length > 0) {
    const directory = pending.pop();
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const entryPath = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        if (!excludeDirectories.has(entry.name)) {
          pending.push(entryPath);
        }
      } else if (entry.isFile() && include(entryPath)) {
        files.push(entryPath);
      }
    }
  }
  return files;
}

function findingLocation(filePath, text, matchIndex) {
  const line = text.slice(0, matchIndex).split(/\r?\n/).length;
  return `${path.relative(repoRoot, filePath)}:${line}`;
}

function firstPatternFinding(filePath, text, pattern) {
  const match = pattern.exec(text);
  pattern.lastIndex = 0;
  return match ? findingLocation(filePath, text, match.index) : undefined;
}

/**
 * 移除经过逐项审计的非 SQL 协议动作，避免把任务控制消息误判为删除语句。
 *
 * @param {string} normalized 统一分隔符后的源码路径
 * @param {string} text Java 源码
 * @returns {string} 用于 SQL 字面量扫描的源码
 */
function withoutAllowlistedNonSqlLiterals(normalized, text) {
  if (
    !normalized.endsWith(
      "/jimuqu-system/src/main/java/com/jimuqu/system/service/ScheduledJobService.java",
    )
  ) {
    return text;
  }
  return text.replace(
    /private\s+static\s+final\s+String\s+ACTION_DELETE\s*=\s*"DELETE";/,
    "",
  );
}

function assertNoMapperSql() {
  const xmlFindings = [];
  for (const filePath of listFilesRecursively(repoRoot, {
    include: (candidate) => candidate.toLowerCase().endsWith(".xml"),
  })) {
    const text = readFileSync(filePath, "utf8");
    const finding = firstPatternFinding(
      filePath,
      text,
      /<\s*(select|insert|update|delete)\b/i,
    );
    if (finding) xmlFindings.push(finding);
  }
  if (xmlFindings.length > 0) {
    throw new Error(
      `Mapper XML SQL is forbidden. Findings: ${xmlFindings.slice(0, 20).join(", ")}`,
    );
  }

  const javaFiles = listFilesRecursively(repoRoot, {
    include: (candidate) => candidate.toLowerCase().endsWith(".java"),
  });
  const annotationFindings = [];
  for (const filePath of javaFiles) {
    const text = readFileSync(filePath, "utf8");
    const finding = firstPatternFinding(
      filePath,
      text,
      /org\.apache\.ibatis\.annotations\.(Select|Insert|Update|Delete|SelectProvider|InsertProvider|UpdateProvider|DeleteProvider)/,
    );
    if (finding) annotationFindings.push(finding);
  }
  if (annotationFindings.length > 0) {
    throw new Error(
      `MyBatis SQL annotations are forbidden. Findings: ${annotationFindings.slice(0, 20).join(", ")}`,
    );
  }

  const sqlLiteralFindings = [];
  for (const filePath of javaFiles) {
    const normalized = filePath.replaceAll("\\", "/");
    if (
      !normalized.includes("/src/main/java/") ||
      normalized.endsWith("/common/core/utils/sql/SqlUtil.java")
    ) {
      continue;
    }
    const text = withoutAllowlistedNonSqlLiterals(
      normalized,
      readFileSync(filePath, "utf8"),
    );
    const finding = firstPatternFinding(
      filePath,
      text,
      /"\s*(select|insert|update|delete|with)(?:\s+|")/i,
    );
    if (finding) sqlLiteralFindings.push(finding);
  }
  if (sqlLiteralFindings.length > 0) {
    throw new Error(
      `Application-layer SQL string literals are forbidden. Findings: ${sqlLiteralFindings.slice(0, 20).join(", ")}`,
    );
  }
}

function xmlAttribute(attributes, name) {
  const match = new RegExp(`\\b${name}="([^"]*)"`).exec(attributes);
  return match?.[1];
}

function assertMavenTestsRan(notBefore) {
  const reportFiles = listFilesRecursively(repoRoot, {
    excludeDirectories: new Set([".git", ".idea", "node_modules", "runtime"]),
    include: (candidate) =>
      path.basename(candidate).startsWith("TEST-") &&
      candidate.toLowerCase().endsWith(".xml") &&
      candidate.replaceAll("\\", "/").includes("/target/surefire-reports/"),
  }).filter(
    (reportPath) => statSync(reportPath).mtimeMs >= notBefore.getTime() - 2000,
  );
  if (reportFiles.length === 0) {
    throw new Error(
      "Maven verify completed without any fresh Surefire XML reports; the test gate did not execute tests.",
    );
  }

  let testCount = 0;
  const reportsBySuite = new Map();
  for (const reportPath of reportFiles) {
    const text = readFileSync(reportPath, "utf8");
    const suiteMatch = /<testsuite\b([^>]*)>/.exec(text);
    if (!suiteMatch) continue;
    const attributes = suiteMatch[1];
    const suiteName = xmlAttribute(attributes, "name");
    const tests = Number(xmlAttribute(attributes, "tests") ?? 0);
    const failures = Number(xmlAttribute(attributes, "failures") ?? 0);
    const errors = Number(xmlAttribute(attributes, "errors") ?? 0);
    testCount += tests;
    if (suiteName) {
      reportsBySuite.set(suiteName, {
        errors,
        failures,
        path: reportPath,
        tests,
        text,
      });
    }
  }
  if (testCount <= 0) {
    throw new Error("Maven verify produced reports but executed zero tests.");
  }

  const requiredSuites = [
    "com.jimuqu.test.coverage.RuntimeRouteInventoryTest",
    "com.jimuqu.test.http.HttpRouteOwnershipTest",
    "com.jimuqu.test.http.HttpAuthorizationCoverageTest",
    "com.jimuqu.test.http.HealthAuthUserHttpContractTest",
    "com.jimuqu.test.http.RbacHttpContractTest",
    "com.jimuqu.test.http.ResourceMonitorHttpContractTest",
    "com.jimuqu.test.http.ConfigurationMessagingHttpContractTest",
    "com.jimuqu.test.http.PushTransportIntegrationTest",
  ];
  const missingSuites = requiredSuites.filter(
    (suiteName) => !reportsBySuite.has(suiteName),
  );
  if (missingSuites.length > 0) {
    throw new Error(
      `Maven verify did not execute required HTTP coverage suite(s): ${missingSuites.join(", ")}`,
    );
  }
  for (const suiteName of requiredSuites) {
    const suite = reportsBySuite.get(suiteName);
    if (suite.tests <= 0 || suite.failures !== 0 || suite.errors !== 0) {
      throw new Error(
        `Required HTTP coverage suite is not green: ${suiteName} (tests=${suite.tests}, failures=${suite.failures}, errors=${suite.errors})`,
      );
    }
  }

  const inventory = reportsBySuite.get(
    "com.jimuqu.test.coverage.RuntimeRouteInventoryTest",
  );
  const countMatch = /^RUNTIME_ROUTE_INVENTORY_END count=(\d+)\s*$/m.exec(
    inventory.text,
  );
  if (!countMatch) {
    throw new Error(
      "Runtime route inventory report did not emit a parseable operation count.",
    );
  }
  const actualHttpOperationCount = Number(countMatch[1]);
  if (actualHttpOperationCount !== expectedHttpOperationCount) {
    throw new Error(
      `Runtime HTTP operation count changed: expected=${expectedHttpOperationCount} actual=${actualHttpOperationCount}`,
    );
  }
  console.log(`Maven test gate executed ${testCount} test(s).`);
  console.log(
    `Runtime HTTP coverage gate executed all required suites for ${actualHttpOperationCount} operation(s).`,
  );
}

function readZipEntries(buffer, label) {
  const minimumEocdSize = 22;
  const minimumOffset = Math.max(0, buffer.length - 65_557);
  let eocdOffset = -1;
  for (
    let index = buffer.length - minimumEocdSize;
    index >= minimumOffset;
    index--
  ) {
    if (buffer.readUInt32LE(index) === 0x06054b50) {
      eocdOffset = index;
      break;
    }
  }
  if (eocdOffset < 0) {
    throw new Error(`Invalid ZIP/JAR archive (EOCD not found): ${label}`);
  }
  const entryCount = buffer.readUInt16LE(eocdOffset + 10);
  const centralDirectoryOffset = buffer.readUInt32LE(eocdOffset + 16);
  if (entryCount === 0xffff || centralDirectoryOffset === 0xffffffff) {
    throw new Error(
      `ZIP64 archives are not supported by the package gate: ${label}`,
    );
  }

  const entries = [];
  let offset = centralDirectoryOffset;
  for (let index = 0; index < entryCount; index++) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error(`Invalid ZIP central directory entry in ${label}.`);
    }
    const compressionMethod = buffer.readUInt16LE(offset + 10);
    const compressedSize = buffer.readUInt32LE(offset + 20);
    const fileNameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    const localHeaderOffset = buffer.readUInt32LE(offset + 42);
    const name = buffer
      .subarray(offset + 46, offset + 46 + fileNameLength)
      .toString("utf8");
    entries.push({
      data() {
        if (buffer.readUInt32LE(localHeaderOffset) !== 0x04034b50) {
          throw new Error(`Invalid ZIP local header for ${label}!/${name}.`);
        }
        const localNameLength = buffer.readUInt16LE(localHeaderOffset + 26);
        const localExtraLength = buffer.readUInt16LE(localHeaderOffset + 28);
        const dataOffset =
          localHeaderOffset + 30 + localNameLength + localExtraLength;
        const compressed = buffer.subarray(
          dataOffset,
          dataOffset + compressedSize,
        );
        if (compressionMethod === 0) return compressed;
        if (compressionMethod === 8) return inflateRawSync(compressed);
        throw new Error(
          `Unsupported ZIP compression method ${compressionMethod} for ${label}!/${name}.`,
        );
      },
      name,
    });
    offset += 46 + fileNameLength + extraLength + commentLength;
  }
  return entries;
}

function classContainsForbiddenSql(entry, displayName, findings) {
  if (
    !entry.name.endsWith(".class") ||
    entry.name.endsWith("/common/core/utils/sql/SqlUtil.class")
  ) {
    return;
  }
  const classText = entry.data().toString("latin1");
  const forbiddenClassConstants = [
    /org\/apache\/ibatis\/annotations\/(Select|Insert|Update|Delete)(Provider)?/,
    /\bselect\s+[^\0\r\n]{0,300}\s+from\b/i,
    /\binsert\s+into\b/i,
    /\bupdate\s+[`"A-Za-z0-9_.]+\s+set\b/i,
    /\bdelete\s+from\b/i,
    /\bwith\s+[`"A-Za-z0-9_]+\s+as\s*\(/i,
  ];
  if (forbiddenClassConstants.some((pattern) => pattern.test(classText))) {
    findings.add(`${displayName} [forbidden SQL constant]`);
  }
}

function assertPackagedArtifactPolicy() {
  if (!isFile(jarPath)) {
    throw new Error(`Backend jar was not produced: ${jarPath}`);
  }
  const outerEntries = readZipEntries(readFileSync(jarPath), jarPath);
  const entryNames = new Set(
    outerEntries.map((entry) => entry.name.toLowerCase()),
  );
  const findings = new Set();
  const requiredLegalEntries = new Map([
    ["LICENSE", ["meta-inf/license", "boot-inf/classes/meta-inf/license"]],
    [
      "THIRD_PARTY_NOTICES.md",
      [
        "meta-inf/third_party_notices.md",
        "boot-inf/classes/meta-inf/third_party_notices.md",
      ],
    ],
  ]);
  for (const [legalFile, candidates] of requiredLegalEntries) {
    if (!candidates.some((candidate) => entryNames.has(candidate))) {
      findings.add(`missing packaged legal notice: ${legalFile}`);
    }
  }

  const outerPatterns = [
    /^BOOT-INF\/lib\/sa-token-apikey-/,
    /^BOOT-INF\/lib\/jimuqu-(ai|generator|workflow|job)-/,
    /^BOOT-INF\/classes\/com\/jimuqu\/(ai|generator|workflow|job)\//,
    /^BOOT-INF\/classes\/com\/jimuqu\/.*\/(apikey|api_key|plugin)\//,
    /^BOOT-INF\/classes\/com\/jimuqu\/.*\/(SysApiKey|SystemMonitor)[^/]*\.class$/,
    /^BOOT-INF\/classes\/.*\/(mapper|mappers)\/.*\.xml$/,
  ];
  const nestedPatterns = [
    /^com\/jimuqu\/(ai|generator|workflow|job)\//,
    /^com\/jimuqu\/.*\/(apikey|api_key|plugin)\//,
    /^com\/jimuqu\/.*\/(SysApiKey|SystemMonitor)[^/]*\.class$/,
    /(^|\/)(mapper|mappers)\/.*\.xml$/,
  ];

  for (const entry of outerEntries) {
    if (outerPatterns.some((pattern) => pattern.test(entry.name))) {
      findings.add(entry.name);
    }
    if (/^BOOT-INF\/classes\/com\/jimuqu\/.+\.class$/.test(entry.name)) {
      classContainsForbiddenSql(entry, entry.name, findings);
    }
    if (!/^BOOT-INF\/lib\/jimuqu-[^/]+\.jar$/.test(entry.name)) continue;

    const nestedEntries = readZipEntries(
      entry.data(),
      `${jarPath}!/${entry.name}`,
    );
    for (const nestedEntry of nestedEntries) {
      const displayName = `${entry.name}!/${nestedEntry.name}`;
      if (nestedPatterns.some((pattern) => pattern.test(nestedEntry.name))) {
        findings.add(displayName);
      }
      if (/^com\/jimuqu\/.+\.class$/.test(nestedEntry.name)) {
        classContainsForbiddenSql(nestedEntry, displayName, findings);
      }
    }
  }
  if (findings.size > 0) {
    throw new Error(
      `Packaged jar contains forbidden modules, Mapper SQL, or application SQL constants: ${[...findings].slice(0, 50).join(", ")}`,
    );
  }
}

function shellDisplay(command, args) {
  return [command, ...args]
    .map((value) => (/\s/.test(value) ? JSON.stringify(value) : value))
    .join(" ");
}

function spawnTool(command, args, options) {
  if (isWindows && /\.(cmd|bat)$/i.test(command)) {
    const quote = (value) => `"${String(value).replaceAll('"', '""')}"`;
    const commandLine = `"${[command, ...args].map(quote).join(" ")}"`;
    const child = spawn(
      process.env.ComSpec ?? "cmd.exe",
      ["/d", "/s", "/c", commandLine],
      {
        ...options,
        windowsVerbatimArguments: true,
        windowsHide: true,
      },
    );
    return child;
  }
  return spawn(command, args, {
    ...options,
    windowsHide: true,
  });
}

function ensureNotInterrupted() {
  if (interrupted) {
    throw new Error(`Full-stack test interrupted by ${interrupted}.`);
  }
}

async function runChecked(
  command,
  args,
  {
    cwd = repoRoot,
    env = process.env,
    logPath,
    capture = false,
    quiet = false,
  } = {},
) {
  ensureNotInterrupted();
  if (logPath) {
    await mkdir(path.dirname(logPath), { recursive: true });
  }
  if (!quiet) {
    console.log(`> ${shellDisplay(command, args)}`);
  }
  const log = logPath ? createWriteStream(logPath, { flags: "a" }) : undefined;
  const child = spawnTool(command, args, {
    cwd,
    env,
    detached: !isWindows,
    stdio: ["ignore", "pipe", "pipe"],
  });
  activeChildren.add(child);
  const captured = [];
  child.stdout.on("data", (chunk) => {
    if (!quiet) process.stdout.write(chunk);
    log?.write(chunk);
    if (capture) captured.push(chunk);
  });
  child.stderr.on("data", (chunk) => {
    if (!quiet) process.stderr.write(chunk);
    log?.write(chunk);
  });

  let code;
  let signal;
  try {
    ({ code, signal } = await new Promise((resolve, reject) => {
      child.once("error", reject);
      child.once("close", (exitCode, exitSignal) =>
        resolve({ code: exitCode, signal: exitSignal }),
      );
    }));
  } finally {
    activeChildren.delete(child);
    if (log) {
      log.end();
      await finished(log);
    }
  }
  ensureNotInterrupted();
  if (code !== 0) {
    throw new Error(
      `Command failed with ${signal ? `signal ${signal}` : `exit code ${code}`}: ${command}`,
    );
  }
  return Buffer.concat(captured).toString("utf8");
}

async function startService(command, args, cwd, env, stdoutPath, stderrPath) {
  ensureNotInterrupted();
  await mkdir(path.dirname(stdoutPath), { recursive: true });
  const stdout = createWriteStream(stdoutPath, { flags: "a" });
  const stderr = createWriteStream(stderrPath, { flags: "a" });
  console.log(`> ${shellDisplay(command, args)}`);
  const child = spawnTool(command, args, {
    cwd,
    env,
    detached: !isWindows,
    stdio: ["ignore", "pipe", "pipe"],
  });
  const service = {
    child,
    stdoutPath,
    stderrPath,
    stdout,
    stderr,
    error: undefined,
  };
  activeChildren.add(child);
  child.stdout.pipe(stdout);
  child.stderr.pipe(stderr);
  child.once("error", (error) => {
    service.error = error;
  });
  stdout.once("error", (error) => {
    service.error ??= error;
  });
  stderr.once("error", (error) => {
    service.error ??= error;
  });
  child.once("close", () => {
    activeChildren.delete(child);
    stdout.end();
    stderr.end();
  });
  await new Promise((resolve, reject) => {
    child.once("spawn", resolve);
    child.once("error", reject);
  });
  return service;
}

async function flushServiceLogs(service) {
  const flush = (stream) =>
    new Promise((resolve, reject) => {
      if (stream.destroyed || stream.writableEnded) {
        resolve();
        return;
      }
      stream.write("", (error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  await Promise.all([flush(service.stdout), flush(service.stderr)]);
}

async function waitForServiceLogs(service) {
  const completion = Promise.allSettled([
    finished(service.stdout),
    finished(service.stderr),
  ]);
  const timedOut = await Promise.race([
    completion.then(() => false),
    delay(serviceLogCloseTimeoutMs).then(() => true),
  ]);
  if (!timedOut) {
    const results = await completion;
    const failure = results.find((result) => result.status === "rejected");
    if (failure) {
      throw failure.reason;
    }
    return;
  }

  service.child.stdout.unpipe(service.stdout);
  service.child.stderr.unpipe(service.stderr);
  service.child.stdout.destroy();
  service.child.stderr.destroy();
  if (!service.stdout.writableEnded) service.stdout.end();
  if (!service.stderr.writableEnded) service.stderr.end();
  await Promise.race([completion, delay(1000)]);
  throw new Error(
    `Service log streams did not close within ${serviceLogCloseTimeoutMs}ms.`,
  );
}

async function logTail(filePath, count = 80) {
  try {
    const lines = (await readFile(filePath, "utf8")).split(/\r?\n/);
    return `[${filePath}]\n${lines.slice(-count).join(os.EOL)}`;
  } catch {
    return `[${filePath}] unavailable`;
  }
}

async function waitForUrl(service, url, responseCheck) {
  const deadline = Date.now() + startupTimeoutSeconds * 1000;
  let lastFailure = "not requested";
  while (Date.now() < deadline) {
    ensureNotInterrupted();
    if (
      service.error ||
      service.child.exitCode !== null ||
      service.child.signalCode !== null
    ) {
      throw new Error(
        `Process exited before it became ready.\n${await logTail(service.stdoutPath)}\n${await logTail(service.stderrPath)}`,
      );
    }
    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(3000),
      });
      if (response.ok && (await responseCheck(response))) {
        return;
      }
      lastFailure = `HTTP ${response.status}`;
    } catch (error) {
      lastFailure = error.message;
    }
    await delay(1000);
  }
  throw new Error(
    `Process did not become ready within ${startupTimeoutSeconds}s: ${url} (${lastFailure})`,
  );
}

async function stopProcessTree(child) {
  const isRunning = () =>
    child?.exitCode === null && child?.signalCode === null;
  const waitForExit = async (timeoutMs) => {
    if (!isRunning()) return true;
    await Promise.race([
      new Promise((resolve) => child.once("close", resolve)),
      delay(timeoutMs),
    ]);
    return !isRunning();
  };

  if (!child?.pid || !isRunning()) {
    return;
  }
  if (isWindows) {
    const attempts = [];
    for (let attempt = 1; attempt <= 3; attempt++) {
      const result = spawnSync(
        "taskkill.exe",
        ["/PID", String(child.pid), "/T", "/F"],
        {
          stdio: "ignore",
          timeout: 10_000,
          windowsHide: true,
        },
      );
      if (await waitForExit(5000)) return;
      attempts.push(
        `${attempt}:status=${result.status ?? "unknown"},error=${result.error?.message ?? "none"}`,
      );
      await delay(500);
    }
    try {
      child.kill("SIGKILL");
    } catch {
      // The final state check below is authoritative.
    }
    if (await waitForExit(5000)) return;
    throw new Error(
      `taskkill failed to stop process tree ${child.pid} after retries (${attempts.join("; ")}).`,
    );
  }

  try {
    process.kill(-child.pid, "SIGTERM");
  } catch (error) {
    if (error.code !== "ESRCH") throw error;
  }
  if (await waitForExit(5000)) return;

  try {
    process.kill(-child.pid, "SIGKILL");
  } catch (error) {
    if (error.code !== "ESRCH") throw error;
  }
  if (!(await waitForExit(5000))) {
    throw new Error(`Process group ${child.pid} did not exit after SIGKILL.`);
  }
}

async function scanOwnedRedisKeys(database) {
  const output = await runChecked(
    redisCli,
    [
      "--raw",
      "-h",
      backendEnvironment.JIMU_TEST_REDIS_HOST,
      "-p",
      backendEnvironment.JIMU_TEST_REDIS_PORT,
      "-n",
      String(database),
      "--scan",
      "--pattern",
      `${redisPrefix}*`,
    ],
    {
      capture: true,
      env: redisEnvironment,
      quiet: true,
    },
  );
  return output
    .split(/\r?\n/)
    .map((key) => key.trim())
    .filter(Boolean);
}

/**
 * 删除一次扫描得到的本次测试专属 Redis 键。
 *
 * @param {number} database Redis 数据库编号
 * @param {string[]} keys 本次测试前缀下仍存在的键
 */
async function deleteOwnedRedisKeys(database, keys) {
  for (let index = 0; index < keys.length; index += 100) {
    const batch = keys.slice(index, index + 100);
    if (batch.some((key) => !key.startsWith(redisPrefix))) {
      throw new Error(
        "Redis scan returned a key outside the run-owned prefix.",
      );
    }
    await runChecked(
      redisCli,
      [
        "--raw",
        "-h",
        backendEnvironment.JIMU_TEST_REDIS_HOST,
        "-p",
        backendEnvironment.JIMU_TEST_REDIS_PORT,
        "-n",
        String(database),
        "DEL",
        ...batch,
      ],
      {
        env: redisEnvironment,
        logPath: path.join(artifactRoot, "redis.log"),
        quiet: true,
      },
    );
  }
}

/**
 * 执行一次 Redis 清理并立即校验，供启动前隔离检查复用。
 *
 * @param {number} database Redis 数据库编号
 */
async function removeOwnedRedisKeys(database) {
  const keys = await scanOwnedRedisKeys(database);
  await deleteOwnedRedisKeys(database, keys);
  const remaining = await filterExistingRedisKeys(
    database,
    await scanOwnedRedisKeys(database),
  );
  if (remaining.length > 0) {
    throw new Error(
      `Redis cleanup left ${remaining.length} run-owned key(s). Key names and prefixes are intentionally omitted.`,
    );
  }
}

/**
 * 在所有测试进程退出后持续清理 Redis，直至连续多次扫描均无残留。
 *
 * @param {number} database Redis 数据库编号
 */
async function removeOwnedRedisKeysUntilStable(database) {
  const deadline = Date.now() + redisCleanupTimeoutMs;
  let stableChecks = 0;
  let remainingCount = 0;

  while (Date.now() <= deadline) {
    const existing = await filterExistingRedisKeys(
      database,
      await scanOwnedRedisKeys(database),
    );
    remainingCount = existing.length;
    if (remainingCount > 0) {
      await deleteOwnedRedisKeys(database, existing);
      stableChecks = 0;
    } else {
      stableChecks++;
      if (stableChecks >= redisCleanupStableChecks) {
        return;
      }
    }
    await delay(redisCleanupPollIntervalMs);
  }

  remainingCount = (
    await filterExistingRedisKeys(
      database,
      await scanOwnedRedisKeys(database),
    )
  ).length;
  throw new Error(
    `Redis cleanup did not reach ${redisCleanupStableChecks} stable empty scans within ${redisCleanupTimeoutMs}ms; ${remainingCount} run-owned key(s) remain. Key names and prefixes are intentionally omitted.`,
  );
}

async function assertNoOwnedRedisKeys(phase) {
  let remainingCount = 0;
  for (const database of new Set([redisDatabase, 0])) {
    remainingCount += (
      await filterExistingRedisKeys(
        database,
        await scanOwnedRedisKeys(database),
      )
    ).length;
  }
  if (remainingCount > 0) {
    throw new Error(
      `Redis cleanup left ${remainingCount} run-owned key(s) after ${phase}. Key names are intentionally omitted.`,
    );
  }
}

async function filterExistingRedisKeys(database, keys) {
  const existing = [];
  for (const key of keys) {
    const result = (
      await runChecked(
        redisCli,
        [
          "--raw",
          "-h",
          backendEnvironment.JIMU_TEST_REDIS_HOST,
          "-p",
          backendEnvironment.JIMU_TEST_REDIS_PORT,
          "-n",
          String(database),
          "EXISTS",
          key,
        ],
        {
          capture: true,
          env: redisEnvironment,
          quiet: true,
        },
      )
    ).trim();
    if (result === "1") {
      existing.push(key);
    } else if (result !== "0") {
      throw new Error(
        `Redis EXISTS returned an unexpected result: ${JSON.stringify(result)}. The key name is intentionally omitted.`,
      );
    }
  }
  return existing;
}

async function assertSeedMenuFrontendContract() {
  const output = await mysqlExecute(
    `SELECT id, parent_id, HEX(menu_name), COALESCE(component, ''), COALESCE(icon, '') FROM \`${databaseName}\`.sys_menu WHERE menu_type IN ('M','C') ORDER BY id;`,
    "mysql-menu-frontend-contract.log",
    true,
  );
  const rows = output
    .split(/\r?\n/)
    .filter((line) => /^\d+\t\d+\t/.test(line))
    .map((line) => line.split("\t", 5));
  if (rows.length === 0) {
    throw new Error(
      "Fresh E2E database did not expose any directory or page menu rows.",
    );
  }

  const rootMenuNames = rows
    .filter((row) => row[1] === "0")
    .map((row) => row[2]);
  const expectedRootMenuNames = [
    "E7B3BBE7BB9FE7AEA1E79086",
    "E7B3BBE7BB9FE79B91E68EA7",
  ];
  if (rootMenuNames.join("|") !== expectedRootMenuNames.join("|")) {
    throw new Error(
      `Backend root menus must be exactly 系统管理 and 系统监控 (UTF-8 hex): ${rootMenuNames.join(", ")}`,
    );
  }

  const specialComponents = new Set(["Layout", "ParentView", "InnerLink"]);
  const missingComponents = [];
  for (const [menuId, , , component] of rows) {
    if (!component || specialComponents.has(component)) continue;
    const componentPath = path.join(
      frontendRoot,
      "src",
      "views",
      ...component.split("/"),
    );
    if (!isFile(`${componentPath}.vue`)) {
      missingComponents.push(`menuId=${menuId} -> ${component}`);
    }
  }
  if (missingComponents.length > 0) {
    throw new Error(
      `Backend menu component(s) do not exist in Bell: ${missingComponents.join(", ")}`,
    );
  }

  const offlineIconsPath = path.join(
    frontendRoot,
    "src",
    "icons",
    "iconify-offline",
    "offline-icons.ts",
  );
  const offlineIconsText = readFileSync(offlineIconsPath, "utf8");
  const offlineIcons = new Set(
    [...offlineIconsText.matchAll(/addIcon\(\s*'([^']+)'/g)].map(
      (match) => match[1],
    ),
  );
  const missingIcons = [
    ...new Set(
      rows
        .map((row) => row[4])
        .filter((icon) => icon && icon !== "#" && !offlineIcons.has(icon)),
    ),
  ].sort();
  if (missingIcons.length > 0) {
    throw new Error(
      `Backend menu icon(s) are missing from Bell's offline icon bundle: ${missingIcons.join(", ")}`,
    );
  }
  console.log(
    `Seed menu contract matches Bell: ${rows.length} directory/page rows, ${offlineIcons.size} offline icons.`,
  );
}

function assertOwnedPath(root, target, label) {
  const relative = path.relative(path.resolve(root), path.resolve(target));
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new Error(`Refusing to remove ${label} outside ${root}: ${target}`);
  }
}

async function resetOssPath() {
  assertOwnedPath(artifactRoot, ossPath, "OSS path");
  await rm(ossPath, { recursive: true, force: true });
  await mkdir(ossPath, { recursive: true });
  ossCreated = true;
}

async function mysqlExecute(sql, logName, capture = false) {
  return runChecked(mysql, [...mysqlArguments, `--execute=${sql}`], {
    capture,
    env: mysqlEnvironment,
    logPath: path.join(artifactRoot, logName),
  });
}

function lastMatchingLine(output, pattern, label) {
  const line = output
    .split(/\r?\n/)
    .map((candidate) => candidate.trim())
    .findLast((candidate) => pattern.test(candidate));
  pattern.lastIndex = 0;
  if (!line) {
    throw new Error(`Could not parse ${label}: ${output.trim()}`);
  }
  return line;
}

function countTextOccurrences(filePaths, needle) {
  let count = 0;
  for (const filePath of filePaths) {
    if (!isFile(filePath)) continue;
    const text = readFileSync(filePath, "utf8");
    let offset = 0;
    while ((offset = text.indexOf(needle, offset)) >= 0) {
      count++;
      offset += needle.length;
    }
  }
  return count;
}

function assertNoSeedScriptErrors(filePaths, phase) {
  const count = countTextOccurrences(filePaths, "执行 SQL 文件失败");
  if (count > 0) {
    throw new Error(
      `AutoTable initialization logged ${count} seed-script failure(s) during ${phase}.`,
    );
  }
}

async function preflight() {
  if (Number(process.versions.node.split(".")[0]) < 22) {
    throw new Error(
      `Node.js 22 or newer is required, actual: ${process.version}`,
    );
  }
  frontendRoot = resolveFrontendRoot();
  await Promise.all([
    assertPortAvailable(backendPort),
    assertPortAvailable(frontendPort),
  ]);
  assertPreflightConfiguration();
  assertNoMapperSql();
  requiredEnvironment("JIMU_TEST_MYSQL_PASSWORD");

  mysql = resolveTool(
    isWindows ? ["mysql.exe"] : ["mysql"],
    isWindows
      ? ["D:\\app\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe"]
      : process.platform === "darwin"
        ? [
            "/opt/homebrew/opt/mysql-client/bin/mysql",
            "/usr/local/opt/mysql-client/bin/mysql",
          ]
        : [],
  );
  redisCli = resolveTool(
    isWindows ? ["redis-cli.exe", "memurai-cli.exe"] : ["redis-cli"],
    isWindows
      ? [
          "D:\\dev\\Redis\\redis-cli.exe",
          "C:\\Program Files\\Memurai\\memurai-cli.exe",
          "C:\\Program Files\\Memurai Developer\\memurai-cli.exe",
        ]
      : process.platform === "darwin"
        ? ["/opt/homebrew/bin/redis-cli", "/usr/local/bin/redis-cli"]
        : [],
  );
  maven = resolveTool(isWindows ? ["mvn.cmd"] : ["mvn"]);
  java = resolveTool(isWindows ? ["java.exe"] : ["java"]);
  corepack = resolveTool(isWindows ? ["corepack.cmd"] : ["corepack"]);
  for (const [command, args] of [
    [mysql, ["--version"]],
    [redisCli, ["--version"]],
    [maven, ["--version"]],
    [java, ["--version"]],
    [corepack, ["--version"]],
  ]) {
    await runChecked(command, args, { quiet: true });
  }

  console.log(
    `Preflight passed: backend=${repoRoot} frontend=${frontendRoot} ports=${backendPort},${frontendPort}; required tools are executable.`,
  );
  console.log(
    "Gate order: mapper SQL policy -> Maven clean verify/HTTP inventory -> packaged JAR -> isolated runtime/seed restart -> pnpm install/lint/typecheck/unit/build -> production preview -> Playwright.",
  );
}

function withoutBackendSecrets(environment) {
  const sanitized = { ...environment };
  for (const name of Object.keys(sanitized)) {
    if (
      name === "MYSQL_PWD" ||
      name === "REDISCLI_AUTH" ||
      /^JIMU_/i.test(name) ||
      /^(MYSQL|REDIS|DB_|DATABASE_)/i.test(name)
    ) {
      delete sanitized[name];
    }
  }
  return sanitized;
}

async function runFullStack() {
  const mysqlHost = environmentOrDefault("JIMU_TEST_MYSQL_HOST", "127.0.0.1");
  const mysqlPort = environmentOrDefault("JIMU_TEST_MYSQL_PORT", "3306");
  const mysqlUser = environmentOrDefault("JIMU_TEST_MYSQL_USER", "root");
  const mysqlPassword = requiredEnvironment("JIMU_TEST_MYSQL_PASSWORD");
  const redisHost = environmentOrDefault("JIMU_TEST_REDIS_HOST", "127.0.0.1");
  const redisPort = environmentOrDefault("JIMU_TEST_REDIS_PORT", "6379");
  const redisPassword = process.env.JIMU_TEST_REDIS_PASSWORD ?? "";

  backendEnvironment = {
    ...process.env,
    MAVEN_OPTS: environmentOrDefault(
      "JIMU_TEST_MAVEN_OPTS",
      environmentOrDefault("MAVEN_OPTS", defaultMavenOptions),
    ),
    JIMU_TEST_SERVER_PORT: String(backendPort),
    JIMU_TEST_MYSQL_HOST: mysqlHost,
    JIMU_TEST_MYSQL_PORT: mysqlPort,
    JIMU_TEST_MYSQL_DATABASE: databaseName,
    JIMU_TEST_MYSQL_USER: mysqlUser,
    JIMU_TEST_MYSQL_PASSWORD: mysqlPassword,
    JIMU_TEST_REDIS_HOST: redisHost,
    JIMU_TEST_REDIS_PORT: redisPort,
    JIMU_TEST_REDIS_DB: String(redisDatabase),
    JIMU_TEST_REDIS_PASSWORD: redisPassword,
    JIMU_TEST_REDIS_PREFIX: redisPrefix,
    JIMU_REDIS_SERVER: `${redisHost}:${redisPort}`,
    JIMU_REDIS_DB: String(redisDatabase),
    JIMU_REDIS_PASSWORD: redisPassword,
    JIMU_REDIS_PREFIX: redisPrefix,
    JIMU_SSE_HEARTBEAT_INTERVAL: "1000",
    JIMU_TEST_OSS_DOMAIN: `http://127.0.0.1:${backendPort}/file/`,
    JIMU_TEST_OSS_PATH: ossPath,
    JIMU_OSS_DOMAIN: `http://127.0.0.1:${backendPort}/file/`,
    JIMU_OSS_PATH: ossPath,
    JIMU_JUSTAUTH_ENABLED: "true",
    JIMU_JUSTAUTH_GITEE_CLIENT_ID: "http-contract-client",
    JIMU_JUSTAUTH_GITEE_CLIENT_SECRET: "http-contract-secret",
    JIMU_JUSTAUTH_GITEE_REDIRECT_URI: `http://127.0.0.1:${frontendPort}/social-callback?source=gitee`,
    JIMU_TEST_SCHEDULED_JOB_PROBE_ENABLED: "true",
  };
  delete backendEnvironment.MYSQL_PWD;
  delete backendEnvironment.REDISCLI_AUTH;

  frontendEnvironment = {
    ...withoutBackendSecrets(process.env),
    PLAYWRIGHT_API_URL: `http://127.0.0.1:${backendPort}`,
    PLAYWRIGHT_BASE_URL: `http://127.0.0.1:${frontendPort}`,
    PLAYWRIGHT_REUSE_EXISTING_SERVER: "true",
    PLAYWRIGHT_WEB_SERVER_COMMAND: `corepack pnpm preview --host 127.0.0.1 --port ${frontendPort} --strictPort`,
    VITE_GLOB_API_URL: "/prod-api",
    VITE_GLOB_ENABLE_ENCRYPT: "true",
    VITE_PORT: String(frontendPort),
    CI: "true",
  };
  mysqlEnvironment = {
    ...process.env,
    MYSQL_PWD: mysqlPassword,
  };
  for (const name of Object.keys(mysqlEnvironment)) {
    if (
      name === "REDISCLI_AUTH" ||
      /^REDIS/i.test(name) ||
      /^JIMU_(TEST_)?REDIS(?:_|$)/i.test(name)
    ) {
      delete mysqlEnvironment[name];
    }
  }
  redisEnvironment = { ...process.env };
  for (const name of Object.keys(redisEnvironment)) {
    if (/^MYSQL/i.test(name) || /^JIMU_(TEST_)?MYSQL(?:_|$)/i.test(name)) {
      delete redisEnvironment[name];
    }
  }
  if (redisPassword) {
    redisEnvironment.REDISCLI_AUTH = redisPassword;
  } else {
    delete redisEnvironment.REDISCLI_AUTH;
  }
  mysqlArguments = [
    "--protocol=TCP",
    `--host=${mysqlHost}`,
    `--port=${mysqlPort}`,
    `--user=${mysqlUser}`,
    "--batch",
    "--skip-column-names",
  ];

  await mkdir(artifactRoot, { recursive: true });
  await resetOssPath();

  const ping = (
    await runChecked(
      redisCli,
      [
        "--raw",
        "-h",
        redisHost,
        "-p",
        redisPort,
        "-n",
        String(redisDatabase),
        "PING",
      ],
      {
        capture: true,
        env: redisEnvironment,
        logPath: path.join(artifactRoot, "redis.log"),
      },
    )
  ).trim();
  if (ping !== "PONG") {
    throw new Error(
      `Redis DB${redisDatabase} PING returned ${JSON.stringify(ping)}.`,
    );
  }
  redisReady = true;
  for (const database of new Set([redisDatabase, 0])) {
    if ((await scanOwnedRedisKeys(database)).length > 0) {
      throw new Error(
        `Generated Redis run namespace is not empty in DB${database}. Key names and prefixes are intentionally omitted.`,
      );
    }
  }

  databaseOwned = true;
  const mavenStartedAt = new Date();
  await runChecked(
    maven,
    [
      "--batch-mode",
      "-Pdev",
      "-Dsolon.env=test",
      "-DskipTests=false",
      "-DforkCount=0",
      "clean",
      "verify",
    ],
    {
      env: backendEnvironment,
      logPath: path.join(artifactRoot, "maven.log"),
    },
  );
  assertMavenTestsRan(mavenStartedAt);
  assertNoSeedScriptErrors(
    [path.join(artifactRoot, "maven.log")],
    "Maven verification",
  );
  await access(jarPath, fsConstants.R_OK);
  assertPackagedArtifactPolicy();

  await mysqlExecute(
    `DROP DATABASE IF EXISTS \`${databaseName}\`; CREATE DATABASE \`${databaseName}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`,
    "mysql-reset.log",
  );
  await removeOwnedRedisKeys(redisDatabase);
  await assertNoOwnedRedisKeys("Maven verification");
  await resetOssPath();

  await assertPortAvailable(backendPort);
  const backendOutLog = path.join(artifactRoot, "backend.out.log");
  const backendErrorLog = path.join(artifactRoot, "backend.err.log");
  backendService = await startService(
    java,
    ["-jar", jarPath, "--solon.env=test"],
    repoRoot,
    backendEnvironment,
    backendOutLog,
    backendErrorLog,
  );
  await waitForUrl(
    backendService,
    `http://127.0.0.1:${backendPort}/auth/code`,
    async (response) => {
      const payload = await response.json();
      return payload.code === 200;
    },
  );

  const redisProbe = await fetch(
    `http://127.0.0.1:${backendPort}/resource/sms/code?phoneNumber=13800000002`,
    { signal: AbortSignal.timeout(10000) },
  );
  const redisProbePayload = await redisProbe.json();
  if (!redisProbe.ok || redisProbePayload.code !== 200) {
    throw new Error(
      `Backend Redis write probe failed: HTTP=${redisProbe.status}, code=${redisProbePayload.code}, msg=${redisProbePayload.msg}`,
    );
  }
  if ((await scanOwnedRedisKeys(redisDatabase)).length === 0) {
    throw new Error(
      "Backend verification-code probe did not create a run-owned Redis key.",
    );
  }
  if ((await scanOwnedRedisKeys(0)).length > 0) {
    throw new Error(
      `Backend wrote run-owned Redis keys to DB0 instead of DB${redisDatabase}.`,
    );
  }

  const seedCountOutput = await mysqlExecute(
    `SELECT COUNT(*) FROM \`${databaseName}\`.sys_user;`,
    "mysql-seed-check.log",
    true,
  );
  const seedCount = Number(
    lastMatchingLine(seedCountOutput, /^\d+$/, "seed user count"),
  );
  if (seedCount !== 7) {
    throw new Error(
      `Fresh E2E database must contain exactly 7 seed users, actual: ${seedCount}`,
    );
  }
  await flushServiceLogs(backendService);
  const precreatedInitCount = countTextOccurrences(
    [backendOutLog, backendErrorLog],
    "AutoTable 已为预先创建的空数据库写入初始化数据",
  );
  if (precreatedInitCount !== 1) {
    throw new Error(
      `Packaged JAR must initialize the pre-created empty database exactly once, actual log count: ${precreatedInitCount}`,
    );
  }

  const forbiddenTableOutput = await mysqlExecute(
    `SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '${databaseName}' AND (TABLE_NAME IN ('sys_job','sys_job_log','sys_plugin','sys_api_key') OR TABLE_NAME REGEXP '^(wf_|flow_|ai_|gen_)');`,
    "mysql-forbidden-table-check.log",
    true,
  );
  const forbiddenTables = forbiddenTableOutput
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) =>
      /^(sys_(job|job_log|plugin|api_key)|wf_|flow_|ai_|gen_)/.test(line),
    );
  if (forbiddenTables.length > 0) {
    throw new Error(
      `Fresh E2E database contains excluded module table(s): ${forbiddenTables.join(", ")}`,
    );
  }

  const primaryKeySql = `SELECT CONCAT(TABLE_NAME, ':', GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '${databaseName}' AND INDEX_NAME = 'PRIMARY' AND TABLE_NAME IN ('sys_role_dept','sys_role_menu','sys_user_post','sys_user_role') GROUP BY TABLE_NAME ORDER BY TABLE_NAME;`;
  const primaryKeyOutput = await mysqlExecute(
    primaryKeySql,
    "mysql-schema-check.log",
    true,
  );
  const actualPrimaryKeys = primaryKeyOutput
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => /^sys_[a-z_]+:/.test(line));
  const expectedPrimaryKeys = [
    "sys_role_dept:role_id,dept_id",
    "sys_role_menu:role_id,menu_id",
    "sys_user_post:user_id,post_id",
    "sys_user_role:user_id,role_id",
  ];
  if (actualPrimaryKeys.join("|") !== expectedPrimaryKeys.join("|")) {
    throw new Error(
      `Fresh E2E database has unexpected RBAC composite primary keys: ${actualPrimaryKeys.join(", ")}`,
    );
  }

  await assertSeedMenuFrontendContract();
  const seedCardinalitySql = `SELECT CONCAT((SELECT COUNT(*) FROM \`${databaseName}\`.sys_user), ':', (SELECT COUNT(*) FROM \`${databaseName}\`.sys_role), ':', (SELECT COUNT(*) FROM \`${databaseName}\`.sys_role_dept), ':', (SELECT COUNT(*) FROM \`${databaseName}\`.sys_menu));`;
  const firstSeedCardinality = lastMatchingLine(
    await mysqlExecute(
      seedCardinalitySql,
      "mysql-first-start-counts.log",
      true,
    ),
    /^\d+:\d+:\d+:\d+$/,
    "first-start seed cardinality",
  );
  if (firstSeedCardinality !== "7:6:1:84") {
    throw new Error(
      `Fresh E2E database has unexpected seed cardinality: ${firstSeedCardinality}`,
    );
  }

  await stopProcessTree(backendService.child);
  await waitForServiceLogs(backendService);
  await delay(250);
  await assertPortAvailable(backendPort);
  const backendRestartOutLog = path.join(
    artifactRoot,
    "backend.restart.out.log",
  );
  const backendRestartErrorLog = path.join(
    artifactRoot,
    "backend.restart.err.log",
  );
  backendService = await startService(
    java,
    ["-jar", jarPath, "--solon.env=test"],
    repoRoot,
    backendEnvironment,
    backendRestartOutLog,
    backendRestartErrorLog,
  );
  await waitForUrl(
    backendService,
    `http://127.0.0.1:${backendPort}/auth/code`,
    async (response) => {
      const payload = await response.json();
      return payload.code === 200;
    },
  );
  const secondSeedCardinality = lastMatchingLine(
    await mysqlExecute(
      seedCardinalitySql,
      "mysql-second-start-counts.log",
      true,
    ),
    /^\d+:\d+:\d+:\d+$/,
    "second-start seed cardinality",
  );
  if (secondSeedCardinality !== firstSeedCardinality) {
    throw new Error(
      `Second startup changed AutoTable seed cardinality: first=${firstSeedCardinality} second=${secondSeedCardinality}`,
    );
  }
  assertNoSeedScriptErrors(
    [
      backendOutLog,
      backendErrorLog,
      backendRestartOutLog,
      backendRestartErrorLog,
    ],
    "first and second packaged-JAR startup",
  );
  console.log(
    `AutoTable restart preserved seed cardinality: ${secondSeedCardinality}`,
  );

  const playwrightInstallArgs = [
    "pnpm",
    "exec",
    "playwright",
    "install",
    ...(process.platform === "linux" && installPlaywrightDependencies
      ? ["--with-deps"]
      : []),
    "chromium",
  ];
  for (const [args, logName] of [
    [["pnpm", "install", "--frozen-lockfile"], "pnpm-install.log"],
    [["pnpm", "lint"], "pnpm-lint.log"],
    [["pnpm", "typecheck"], "pnpm-typecheck.log"],
    [["pnpm", "test:unit"], "pnpm-unit.log"],
    [["pnpm", "build"], "pnpm-build.log"],
    [playwrightInstallArgs, "playwright-install.log"],
  ]) {
    await runChecked(corepack, args, {
      cwd: frontendRoot,
      env: frontendEnvironment,
      logPath: path.join(artifactRoot, logName),
    });
  }

  const runtimeConfigName = readdirSync(path.join(frontendRoot, "dist")).find(
    (name) => /^_app-config-.*\.js$/.test(name),
  );
  if (!runtimeConfigName) {
    throw new Error("Frontend runtime config was not generated.");
  }
  const runtimeConfigText = readFileSync(
    path.join(frontendRoot, "dist", runtimeConfigName),
    "utf8",
  );
  if (!runtimeConfigText.includes('"VITE_GLOB_API_URL":"/prod-api"')) {
    throw new Error(
      "Frontend runtime config does not contain the production API prefix: /prod-api",
    );
  }
  if (runtimeConfigText.includes(`http://127.0.0.1:${backendPort}`)) {
    throw new Error(
      "Frontend runtime config leaked the Playwright backend proxy target.",
    );
  }
  if (!runtimeConfigText.includes('"VITE_GLOB_ENABLE_ENCRYPT":"true"')) {
    throw new Error(
      "Frontend runtime config did not enable transport encryption for the full-stack test.",
    );
  }

  await assertPortAvailable(frontendPort);
  const viteCli = path.join(
    frontendRoot,
    "node_modules",
    "vite",
    "bin",
    "vite.js",
  );
  if (!isFile(viteCli)) {
    throw new Error(`Vite CLI was not installed: ${viteCli}`);
  }
  frontendService = await startService(
    process.execPath,
    [
      viteCli,
      "preview",
      "--host",
      "127.0.0.1",
      "--port",
      String(frontendPort),
      "--strictPort",
    ],
    frontendRoot,
    frontendEnvironment,
    path.join(artifactRoot, "frontend.out.log"),
    path.join(artifactRoot, "frontend.err.log"),
  );
  await waitForUrl(
    frontendService,
    `http://127.0.0.1:${frontendPort}/`,
    async () => true,
  );
  await runChecked(
    corepack,
    ["pnpm", "exec", "playwright", "test", "--config=playwright.config.ts"],
    {
      cwd: frontendRoot,
      env: frontendEnvironment,
      logPath: path.join(artifactRoot, "playwright.log"),
    },
  );

  console.log(`Full-stack tests passed. Logs: ${artifactRoot}`);
}

async function cleanup() {
  const receivedSignal = interrupted;
  interrupted = undefined;
  const failures = [];
  try {
    for (const service of [frontendService, backendService]) {
      try {
        await stopProcessTree(service?.child);
        if (service) await waitForServiceLogs(service);
      } catch (error) {
        failures.push(`Process cleanup failed: ${error.message}`);
      }
    }
    for (const child of [...activeChildren]) {
      try {
        await stopProcessTree(child);
      } catch (error) {
        failures.push(`Child process cleanup failed: ${error.message}`);
      }
    }
    await delay(250);
    for (const port of [backendPort, frontendPort]) {
      try {
        await assertPortAvailable(port);
      } catch (error) {
        failures.push(
          `Port ${port} is still listening after cleanup: ${error.message}`,
        );
      }
    }
    if (redisReady) {
      for (const database of [redisDatabase, 0]) {
        try {
          await removeOwnedRedisKeysUntilStable(database);
        } catch (error) {
          failures.push(`Redis DB${database} cleanup failed: ${error.message}`);
        }
      }
      try {
        await assertNoOwnedRedisKeys("full-stack verification");
      } catch (error) {
        failures.push(
          `Redis DB${redisDatabase} isolation cleanup failed: ${error.message}`,
        );
      }
    }
    if (
      databaseOwned &&
      mysql &&
      /^jimuqu_it_\d{17}_\d+_[0-9a-f]{8}$/.test(databaseName)
    ) {
      try {
        await mysqlExecute(
          `DROP DATABASE IF EXISTS \`${databaseName}\`;`,
          "mysql-cleanup.log",
        );
        databaseOwned = false;
      } catch (error) {
        failures.push(
          `Database cleanup failed for ${databaseName}: ${error.message}`,
        );
      }
    }
    if (ossCreated && existsSync(ossPath)) {
      try {
        assertOwnedPath(artifactRoot, ossPath, "OSS path");
        await rm(ossPath, { recursive: true, force: true });
      } catch (error) {
        failures.push(`Temporary OSS cleanup failed: ${error.message}`);
      }
    }
  } finally {
    interrupted = receivedSignal;
  }
  return failures;
}

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.once(signal, () => {
    interrupted = signal;
    for (const child of activeChildren) {
      void stopProcessTree(child).catch((error) => {
        console.error(
          `Failed to stop child process ${child.pid ?? "unknown"} after ${signal}: ${error.message}`,
        );
      });
    }
  });
}

let failure;
try {
  await preflight();
  if (preflightOnly) {
    console.log(
      "Preflight-only mode completed without creating a database, touching Redis, or starting a service.",
    );
  } else {
    await runFullStack();
  }
} catch (error) {
  failure = error;
} finally {
  const cleanupFailures = await cleanup();
  if (cleanupFailures.length > 0) {
    const cleanupError = new Error(cleanupFailures.join(os.EOL));
    if (failure) {
      console.error(`Cleanup also failed:${os.EOL}${cleanupError.message}`);
    } else {
      failure = cleanupError;
    }
  }
  if (!failure && interrupted) {
    failure = new Error(`Full-stack test interrupted by ${interrupted}.`);
  }
}

if (failure) {
  console.error(failure.stack ?? failure);
  process.exitCode = 1;
}
