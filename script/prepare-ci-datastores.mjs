#!/usr/bin/env node

import { spawn } from "node:child_process";
import { accessSync, constants as fsConstants, statSync } from "node:fs";
import path from "node:path";
import process from "node:process";
import { setTimeout as delay } from "node:timers/promises";

const isWindows = process.platform === "win32";
const mysqlUser = requiredEnvironment("JIMU_TEST_MYSQL_USER");
const mysqlPassword = requiredEnvironment("JIMU_TEST_MYSQL_PASSWORD");
const mysqlHost = process.env.JIMU_TEST_MYSQL_HOST ?? "127.0.0.1";
const mysqlPort = process.env.JIMU_TEST_MYSQL_PORT ?? "3306";
const redisHost = process.env.JIMU_TEST_REDIS_HOST ?? "127.0.0.1";
const redisPort = process.env.JIMU_TEST_REDIS_PORT ?? "6379";

if (process.env.GITHUB_ACTIONS !== "true") {
  throw new Error("This helper may only run inside GitHub Actions.");
}
if (!/^[A-Za-z0-9_]{1,32}$/.test(mysqlUser)) {
  throw new Error("JIMU_TEST_MYSQL_USER contains unsupported characters.");
}
if (!/^[A-Za-z0-9._-]{12,128}$/.test(mysqlPassword)) {
  throw new Error(
    "JIMU_TEST_MYSQL_PASSWORD must contain 12-128 portable characters.",
  );
}

const mysql = resolveTool(
  isWindows ? ["mysql.exe"] : ["mysql"],
  isWindows
    ? [
        "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe",
        "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe",
      ]
    : process.platform === "darwin"
      ? [
          "/opt/homebrew/opt/mysql-client/bin/mysql",
          "/usr/local/opt/mysql-client/bin/mysql",
        ]
      : [],
);
const redisCli = resolveTool(
  isWindows ? ["redis-cli.exe", "memurai-cli.exe"] : ["redis-cli"],
  isWindows
    ? [
        "C:\\Program Files\\Memurai\\memurai-cli.exe",
        "C:\\Program Files\\Memurai Developer\\memurai-cli.exe",
      ]
    : process.platform === "darwin"
      ? ["/opt/homebrew/bin/redis-cli", "/usr/local/bin/redis-cli"]
      : [],
);

await ensureRedisReady();
await configureMysqlPassword();
console.log("CI MySQL and Redis services are ready.");

function requiredEnvironment(name) {
  const value = process.env[name];
  if (!value?.trim()) {
    throw new Error(`Required environment variable is missing: ${name}`);
  }
  return value;
}

function isExecutableFile(filePath) {
  try {
    if (!statSync(filePath).isFile()) {
      return false;
    }
    if (!isWindows) {
      accessSync(filePath, fsConstants.X_OK);
    }
    return true;
  } catch {
    return false;
  }
}

function resolveTool(names, candidates = []) {
  const searchDirectories = (process.env.PATH ?? "")
    .split(path.delimiter)
    .map((entry) => entry.replace(/^"(.*)"$/, "$1"))
    .filter(Boolean);
  for (const candidate of [
    ...candidates,
    ...searchDirectories.flatMap((directory) =>
      names.map((name) => path.join(directory, name)),
    ),
  ]) {
    if (isExecutableFile(candidate)) {
      return path.resolve(candidate);
    }
  }
  throw new Error(`Required CI tool was not found: ${names.join(" or ")}`);
}

async function run(command, args, { env = process.env, input } = {}) {
  const child = spawn(command, args, {
    env,
    stdio: ["pipe", "pipe", "pipe"],
    windowsHide: true,
  });
  const stdout = [];
  const stderr = [];
  child.stdout.on("data", (chunk) => stdout.push(chunk));
  child.stderr.on("data", (chunk) => stderr.push(chunk));
  if (input === undefined) {
    child.stdin.end();
  } else {
    child.stdin.end(input);
  }
  const code = await new Promise((resolve, reject) => {
    child.once("error", reject);
    child.once("close", resolve);
  });
  return {
    code,
    stderr: Buffer.concat(stderr).toString("utf8").trim(),
    stdout: Buffer.concat(stdout).toString("utf8").trim(),
  };
}

async function retry(label, operation) {
  let lastMessage = "not ready";
  for (let attempt = 1; attempt <= 60; attempt++) {
    const result = await operation();
    if (result.code === 0) {
      return result;
    }
    lastMessage = result.stderr || result.stdout || `exit code ${result.code}`;
    if (attempt < 60) {
      await delay(1000);
    }
  }
  throw new Error(`${label} did not become ready: ${lastMessage}`);
}

async function ensureRedisReady() {
  let result = await run(redisCli, [
    "--raw",
    "-h",
    redisHost,
    "-p",
    redisPort,
    "PING",
  ]);
  if (result.code !== 0 && isWindows) {
    await run("sc.exe", ["start", "Memurai"]);
  }
  result = await retry("Redis", () =>
    run(redisCli, [
      "--raw",
      "-h",
      redisHost,
      "-p",
      redisPort,
      "PING",
    ]),
  );
  if (result.stdout !== "PONG") {
    throw new Error(`Redis PING returned ${JSON.stringify(result.stdout)}.`);
  }
}

async function configureMysqlPassword() {
  const baseArguments = [
    "--protocol=TCP",
    `--host=${mysqlHost}`,
    `--port=${mysqlPort}`,
    `--user=${mysqlUser}`,
    "--batch",
    "--skip-column-names",
  ];
  const passwordlessEnvironment = { ...process.env };
  delete passwordlessEnvironment.MYSQL_PWD;

  await retry("MySQL", () =>
    run(mysql, [...baseArguments, "--execute=SELECT 1"], {
      env: passwordlessEnvironment,
    }),
  );
  const alterResult = await run(mysql, baseArguments, {
    env: passwordlessEnvironment,
    input: `ALTER USER USER() IDENTIFIED BY '${mysqlPassword}';\n`,
  });
  if (alterResult.code !== 0) {
    throw new Error(
      `Unable to configure the CI MySQL account: ${alterResult.stderr || `exit code ${alterResult.code}`}`,
    );
  }

  const authenticatedEnvironment = {
    ...process.env,
    MYSQL_PWD: mysqlPassword,
  };
  const verifyResult = await run(
    mysql,
    [...baseArguments, "--execute=SELECT 1"],
    { env: authenticatedEnvironment },
  );
  if (verifyResult.code !== 0 || verifyResult.stdout !== "1") {
    throw new Error(
      `Unable to authenticate the configured CI MySQL account: ${verifyResult.stderr || `exit code ${verifyResult.code}`}`,
    );
  }
}
