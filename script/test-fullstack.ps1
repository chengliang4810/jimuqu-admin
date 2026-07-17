[CmdletBinding()]
param(
    [int]$StartupTimeoutSeconds = 180,
    [string]$FrontendDir = $env:JIMU_TEST_FRONTEND_DIR,
    [switch]$PreflightOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-EnvOrDefault {
    param([string]$Name, [string]$Default)

    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    return $value
}

function Get-RequiredEnv {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable is missing: $Name"
    }
    return $value
}

function Resolve-Tool {
    param([string]$Name, [string[]]$Candidates = @())

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -ne $command) { return $command.Source }
    foreach ($candidate in $Candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return (Get-Item -LiteralPath $candidate).FullName
        }
    }
    throw "Required tool was not found: $Name"
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [string]$LogPath
    )

    Write-Host "> $FilePath $($Arguments -join ' ')"
    $exitCode = 0
    Push-Location $WorkingDirectory
    try {
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & $FilePath @Arguments 2>&1 | Tee-Object -FilePath $LogPath -Append | Out-Host
            $exitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) {
        throw "Command failed with exit code ${exitCode}: $FilePath"
    }
}

function Invoke-CapturedChecked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [string]$LogPath
    )

    Write-Host "> $FilePath $($Arguments -join ' ')"
    $exitCode = 0
    $output = @()
    Push-Location $WorkingDirectory
    try {
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $output = @(& $FilePath @Arguments 2>&1)
            $exitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        $output | Tee-Object -FilePath $LogPath -Append | Out-Host
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) {
        throw "Command failed with exit code ${exitCode}: $FilePath"
    }
    return $output
}

function Get-PortListeners {
    param([int]$Port)

    return @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Assert-PortAvailable {
    param([int]$Port)

    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
    $probe.Server.ExclusiveAddressUse = $true
    try {
        $probe.Start()
    } catch [Net.Sockets.SocketException] {
        throw "Port $Port is already in use. The test runner never reuses an existing service."
    } finally {
        $probe.Stop()
    }
}

function Wait-ForBackend {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$Url,
        [int]$TimeoutSeconds,
        [string[]]$LogPaths
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastFailure = ''
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) {
            $tails = foreach ($logPath in $LogPaths) {
                if (Test-Path -LiteralPath $logPath) {
                    "[$logPath]`n$((Get-Content -LiteralPath $logPath -Tail 80) -join [Environment]::NewLine)"
                }
            }
            throw "Backend exited before it became ready.`n$($tails -join [Environment]::NewLine)"
        }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                $payload = $response.Content | ConvertFrom-Json
                if ($payload.code -eq 200) { return }
                $lastFailure = "Unexpected business code: $($payload.code)"
            } else {
                $lastFailure = "Unexpected HTTP status: $($response.StatusCode)"
            }
        } catch {
            $lastFailure = $_.Exception.Message
        }
        Start-Sleep -Seconds 1
    }
    throw "Backend did not satisfy the readiness contract within $TimeoutSeconds seconds: $Url ($lastFailure)"
}

function Wait-ForProcessUrl {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$Url,
        [int]$TimeoutSeconds,
        [string[]]$LogPaths
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastFailure = ''
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) {
            $tails = foreach ($logPath in $LogPaths) {
                if (Test-Path -LiteralPath $logPath) {
                    "[$logPath]`n$((Get-Content -LiteralPath $logPath -Tail 80) -join [Environment]::NewLine)"
                }
            }
            throw "Process exited before it became ready.`n$($tails -join [Environment]::NewLine)"
        }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { return }
            $lastFailure = "Unexpected HTTP status: $($response.StatusCode)"
        } catch {
            $lastFailure = $_.Exception.Message
        }
        Start-Sleep -Seconds 1
    }
    throw "Process did not become ready within $TimeoutSeconds seconds: $Url ($lastFailure)"
}

function Assert-TestConfiguration {
    param([string]$RepoRoot, [string]$FrontendRoot)

    $backendConfig = Join-Path $RepoRoot 'jimuqu-admin\src\main\resources\app-test.yml'
    if (-not (Test-Path -LiteralPath $backendConfig -PathType Leaf)) {
        throw "Test configuration is missing: $backendConfig"
    }
    $backendText = Get-Content -Raw -LiteralPath $backendConfig
    $requiredBackendMarkers = @(
        '${JIMU_TEST_SERVER_PORT}',
        '${JIMU_TEST_MYSQL_DATABASE}',
        '${JIMU_TEST_REDIS_DB}',
        '${JIMU_TEST_REDIS_PREFIX}',
        '${JIMU_TEST_OSS_PATH}'
    )
    foreach ($marker in $requiredBackendMarkers) {
        if ($backendText.IndexOf($marker, [StringComparison]::Ordinal) -lt 0) {
            throw "Test configuration does not consume required isolation marker ${marker}: $backendConfig"
        }
    }

    $frontendPackagePath = Join-Path $FrontendRoot 'package.json'
    $playwrightConfig = Join-Path $FrontendRoot 'playwright.config.ts'
    $smokeTest = Join-Path $FrontendRoot 'tests\e2e\smoke.spec.ts'
    foreach ($path in @($frontendPackagePath, $playwrightConfig, $smokeTest)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Frontend test prerequisite is missing: $path"
        }
    }
    $frontendPackage = Get-Content -Raw -LiteralPath $frontendPackagePath | ConvertFrom-Json
    if ($frontendPackage.name -ne 'jimuqu-admin-ui') {
        throw "Frontend is not the Bell-Plus tree; refusing to run E2E against $($frontendPackage.name)."
    }
}

function Assert-NoMapperSql {
    param([string]$RepoRoot)

    $xmlFindings = Get-ChildItem -LiteralPath $RepoRoot -Recurse -Filter '*.xml' -File |
        Where-Object { $_.FullName -notmatch '\\target\\' } |
        Select-String -Pattern '<\s*(select|insert|update|delete)\b' -CaseSensitive:$false
    if ($null -ne $xmlFindings) {
        $summary = ($xmlFindings | Select-Object -First 20 | ForEach-Object { "$($_.Path):$($_.LineNumber)" }) -join ', '
        throw "Mapper XML SQL is forbidden. Findings: $summary"
    }

    $annotationPattern = 'org\.apache\.ibatis\.annotations\.(Select|Insert|Update|Delete|SelectProvider|InsertProvider|UpdateProvider|DeleteProvider)'
    $annotationFindings = Get-ChildItem -LiteralPath $RepoRoot -Recurse -Filter '*.java' -File |
        Where-Object { $_.FullName -notmatch '\\target\\' } |
        Select-String -Pattern $annotationPattern
    if ($null -ne $annotationFindings) {
        $summary = ($annotationFindings | Select-Object -First 20 | ForEach-Object { "$($_.Path):$($_.LineNumber)" }) -join ', '
        throw "MyBatis SQL annotations are forbidden. Findings: $summary"
    }
}

function Assert-MavenTestsRan {
    param([string]$RepoRoot)

    $reports = @(Get-ChildItem -LiteralPath $RepoRoot -Recurse -Filter 'TEST-*.xml' -File |
        Where-Object { $_.FullName -match '\\target\\surefire-reports\\' })
    if ($reports.Count -eq 0) {
        throw 'Maven verify completed without any Surefire XML reports; the test gate did not execute tests.'
    }
    $testCount = 0
    foreach ($report in $reports) {
        [xml]$document = Get-Content -Raw -LiteralPath $report.FullName
        $testCount += [int]$document.testsuite.tests
    }
    if ($testCount -le 0) {
        throw 'Maven verify produced reports but executed zero tests.'
    }
    Write-Host "Maven test gate executed $testCount test(s)."
}

function Get-RedisKeys {
    param(
        [string]$RedisCli,
        [string[]]$RedisArguments,
        [string]$Pattern
    )

    $keys = @(& $RedisCli @RedisArguments --scan --pattern $Pattern 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "redis-cli scan failed with exit code $LASTEXITCODE"
    }
    return @($keys | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Remove-OwnedRedisKeys {
    param(
        [string]$RedisCli,
        [string[]]$RedisArguments,
        [string]$Prefix
    )

    $keys = @(Get-RedisKeys $RedisCli $RedisArguments "$Prefix*")
    foreach ($key in $keys) {
        if (-not $key.StartsWith($Prefix, [StringComparison]::Ordinal)) {
            throw "Redis scan returned a key outside the owned prefix: $key"
        }
        & $RedisCli @RedisArguments UNLINK $key | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "redis-cli UNLINK failed for key $key" }
    }
    $remainingKeys = @(Get-RedisKeys $RedisCli $RedisArguments "$Prefix*")
    if ($remainingKeys.Count -gt 0) {
        throw "Redis cleanup left $($remainingKeys.Count) key(s) under $Prefix"
    }
    return $keys.Count
}

function Reset-OwnedOssPath {
    param([string]$ArtifactRoot, [string]$OssPath)

    $fullArtifactRoot = [IO.Path]::GetFullPath($ArtifactRoot).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $fullOssPath = [IO.Path]::GetFullPath($OssPath)
    if (-not $fullOssPath.StartsWith($fullArtifactRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to reset OSS path outside the test artifact directory: $fullOssPath"
    }
    if (Test-Path -LiteralPath $fullOssPath) {
        Remove-Item -LiteralPath $fullOssPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $fullOssPath -Force | Out-Null
}

function Stop-TestFrontendListeners {
    param(
        [int]$Port,
        [string]$FrontendRoot,
        [DateTime]$RunStartedAt
    )

    $failures = [System.Collections.Generic.List[string]]::new()
    foreach ($listener in @(Get-PortListeners $Port)) {
        $ownerPid = [int]$listener.OwningProcess
        $process = Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerPid" -ErrorAction SilentlyContinue
        $commandLine = if ($null -eq $processInfo) { '' } else { [string]$processInfo.CommandLine }
        $startedByThisRun = $null -ne $process -and $process.StartTime -ge $RunStartedAt.AddSeconds(-5)
        $looksLikePreview = $commandLine -match '(?i)(vite(\.js)?\s+preview|pnpm(\.c?m?d)?\s+preview)'
        $belongsToFrontend = $commandLine.IndexOf($FrontendRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0
        if ($startedByThisRun -and $looksLikePreview -and $belongsToFrontend) {
            Stop-Process -Id $ownerPid -Force -ErrorAction SilentlyContinue
            if ($null -ne $process) { $process.WaitForExit(10000) | Out-Null }
        } else {
            $failures.Add("Refusing to stop unowned listener on port ${Port}, PID ${ownerPid}: $commandLine")
        }
    }
    Start-Sleep -Milliseconds 250
    if (@(Get-PortListeners $Port).Count -gt 0 -and $failures.Count -eq 0) {
        $failures.Add("Port $Port is still listening after frontend cleanup.")
    }
    return $failures
}

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($FrontendDir)) {
    $FrontendDir = Join-Path ([IO.Path]::GetPathRoot($repoRoot)) 'WebProjects\jimuqu-admin-ui'
}
$FrontendDir = [IO.Path]::GetFullPath($FrontendDir)

$backendPort = 15320
$frontendPort = 15555
$redisDatabase = 15
$runStartedAt = Get-Date
$runId = '{0}_{1}' -f $runStartedAt.ToString('yyyyMMddHHmmssfff'), $PID
$databaseName = "jimuqu_it_$runId"
$redisPrefix = "jimu:it:${runId}:"
$artifactRoot = Join-Path $repoRoot "runtime\test\$runId"
$ossPath = Join-Path $artifactRoot 'oss'
$backendOutLog = Join-Path $artifactRoot 'backend.out.log'
$backendErrorLog = Join-Path $artifactRoot 'backend.err.log'

$environmentNames = @(
    'JIMU_TEST_SERVER_PORT',
    'JIMU_TEST_MYSQL_HOST',
    'JIMU_TEST_MYSQL_PORT',
    'JIMU_TEST_MYSQL_DATABASE',
    'JIMU_TEST_MYSQL_USER',
    'JIMU_TEST_MYSQL_PASSWORD',
    'JIMU_TEST_REDIS_HOST',
    'JIMU_TEST_REDIS_PORT',
    'JIMU_TEST_REDIS_DB',
    'JIMU_TEST_REDIS_PASSWORD',
    'JIMU_TEST_REDIS_PREFIX',
    'JIMU_TEST_OSS_DOMAIN',
    'JIMU_TEST_OSS_PATH',
    'PLAYWRIGHT_API_URL',
    'PLAYWRIGHT_BASE_URL',
    'PLAYWRIGHT_REUSE_EXISTING_SERVER',
    'PLAYWRIGHT_WEB_SERVER_COMMAND',
    'VITE_GLOB_API_URL',
    'VITE_GLOB_ENABLE_ENCRYPT',
    'VITE_PORT',
    'CI',
    'REDISCLI_AUTH',
    'MYSQL_PWD'
)
$savedEnvironment = @{}
foreach ($name in $environmentNames) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$backendProcess = $null
$frontendProcess = $null
$playwrightStarted = $false
$databaseCreated = $false
$redisReady = $false
$mysql = $null
$redisCli = $null
$mysqlArgs = @()
$redisArgs = @()
$runFailure = $null
$cleanupFailures = [System.Collections.Generic.List[string]]::new()
$runMutex = [Threading.Mutex]::new($false, 'Local\jimuqu-admin-fullstack-15320-15555')
$mutexAcquired = $false

try {
    try {
        $mutexAcquired = $runMutex.WaitOne(0)
    } catch [Threading.AbandonedMutexException] {
        $mutexAcquired = $true
    }
    if (-not $mutexAcquired) {
        throw 'Another jimuqu full-stack test run already owns ports 15320 and 15555.'
    }

    Assert-PortAvailable $backendPort
    Assert-PortAvailable $frontendPort
    if (-not (Test-Path -LiteralPath $FrontendDir -PathType Container)) {
        throw "Frontend repository was not found: $FrontendDir"
    }
    Assert-TestConfiguration $repoRoot $FrontendDir

    $mysqlHost = Get-EnvOrDefault 'JIMU_TEST_MYSQL_HOST' '127.0.0.1'
    $mysqlPort = Get-EnvOrDefault 'JIMU_TEST_MYSQL_PORT' '3306'
    $mysqlUser = Get-EnvOrDefault 'JIMU_TEST_MYSQL_USER' 'root'
    $mysqlPassword = Get-RequiredEnv 'JIMU_TEST_MYSQL_PASSWORD'
    $redisHost = Get-EnvOrDefault 'JIMU_TEST_REDIS_HOST' '127.0.0.1'
    $redisPort = Get-EnvOrDefault 'JIMU_TEST_REDIS_PORT' '6379'
    $redisPassword = Get-EnvOrDefault 'JIMU_TEST_REDIS_PASSWORD' ''

    $mysql = Resolve-Tool 'mysql.exe' @('D:\app\MySQL\MySQL Server 8.0\bin\mysql.exe')
    $redisCli = Resolve-Tool 'redis-cli.exe' @('D:\dev\Redis\redis-cli.exe')
    $maven = Resolve-Tool 'mvn.cmd'
    $java = Resolve-Tool 'java.exe'
    $corepack = Resolve-Tool 'corepack.cmd'

    Write-Host "Preflight passed: backend=$repoRoot frontend=$FrontendDir MySQL=${mysqlHost}:$mysqlPort Redis=${redisHost}:$redisPort/DB${redisDatabase} ports=${backendPort},${frontendPort}"
    Write-Host 'Gate order: mapper SQL policy -> Maven clean verify -> backend readiness -> pnpm install/lint/typecheck/unit/build -> Playwright Chromium.'
    if ($PreflightOnly) {
        Write-Host 'Preflight-only mode completed without creating a database, touching Redis, or starting a service.'
        return
    }

    New-Item -ItemType Directory -Path $ossPath -Force | Out-Null
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $mysqlPassword, 'Process')
    if (-not [string]::IsNullOrEmpty($redisPassword)) {
        [Environment]::SetEnvironmentVariable('REDISCLI_AUTH', $redisPassword, 'Process')
    } else {
        [Environment]::SetEnvironmentVariable('REDISCLI_AUTH', $null, 'Process')
        Remove-Item Env:REDISCLI_AUTH -ErrorAction SilentlyContinue
    }

    $mysqlArgs = @(
        '--protocol=TCP',
        "--host=$mysqlHost",
        "--port=$mysqlPort",
        "--user=$mysqlUser",
        '--batch',
        '--skip-column-names'
    )
    $redisArgs = @('--raw', '-h', $redisHost, '-p', $redisPort, '-n', "$redisDatabase")

    Invoke-Checked $mysql ($mysqlArgs + "--execute=CREATE DATABASE $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;") $repoRoot (Join-Path $artifactRoot 'mysql-create.log')
    $databaseCreated = $true

    $pingOutput = @(Invoke-CapturedChecked $redisCli ($redisArgs + 'PING') $repoRoot (Join-Path $artifactRoot 'redis.log'))
    $ping = ($pingOutput |
        Where-Object { $_ -is [string] -and -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Last 1).Trim()
    if ($ping -ne 'PONG') { throw "Redis DB${redisDatabase} PING returned '$ping' instead of PONG." }
    $redisReady = $true
    $preexistingKeys = @(Get-RedisKeys $redisCli $redisArgs "$redisPrefix*")
    if ($preexistingKeys.Count -gt 0) {
        throw "The unique Redis prefix unexpectedly already contains $($preexistingKeys.Count) key(s): $redisPrefix"
    }

    $testEnvironment = @{
        JIMU_TEST_SERVER_PORT = "$backendPort"
        JIMU_TEST_MYSQL_HOST = $mysqlHost
        JIMU_TEST_MYSQL_PORT = $mysqlPort
        JIMU_TEST_MYSQL_DATABASE = $databaseName
        JIMU_TEST_MYSQL_USER = $mysqlUser
        JIMU_TEST_MYSQL_PASSWORD = $mysqlPassword
        JIMU_TEST_REDIS_HOST = $redisHost
        JIMU_TEST_REDIS_PORT = $redisPort
        JIMU_TEST_REDIS_DB = "$redisDatabase"
        JIMU_TEST_REDIS_PASSWORD = $redisPassword
        JIMU_TEST_REDIS_PREFIX = $redisPrefix
        JIMU_TEST_OSS_DOMAIN = "http://127.0.0.1:$backendPort/file/"
        JIMU_TEST_OSS_PATH = $ossPath
        PLAYWRIGHT_API_URL = "http://127.0.0.1:$backendPort"
        PLAYWRIGHT_BASE_URL = "http://127.0.0.1:$frontendPort"
        PLAYWRIGHT_REUSE_EXISTING_SERVER = 'true'
        PLAYWRIGHT_WEB_SERVER_COMMAND = "corepack pnpm preview --host 127.0.0.1 --port $frontendPort --strictPort"
        VITE_GLOB_API_URL = '/prod-api'
        VITE_GLOB_ENABLE_ENCRYPT = 'false'
        VITE_PORT = "$frontendPort"
        CI = 'true'
    }
    foreach ($entry in $testEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, [string]$entry.Value, 'Process')
    }

    Assert-NoMapperSql $repoRoot
    Invoke-Checked $maven @(
        '-Pdev',
        '-Dsolon.env=test',
        '-DskipTests=false',
        '-DforkCount=0',
        'clean',
        'verify'
    ) $repoRoot (Join-Path $artifactRoot 'maven.log')
    Assert-MavenTestsRan $repoRoot

    if ($databaseName -notmatch '^jimuqu_it_\d{17}_\d+$') {
        throw "Refusing to reset database with an unexpected generated name: $databaseName"
    }
    Write-Host 'Resetting the Maven-mutated database, Redis namespace and OSS directory before browser E2E.'
    Invoke-Checked $mysql ($mysqlArgs + "--execute=DROP DATABASE $databaseName; CREATE DATABASE $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;") $repoRoot (Join-Path $artifactRoot 'mysql-reset.log')
    $removedRedisKeys = Remove-OwnedRedisKeys $redisCli $redisArgs $redisPrefix
    Reset-OwnedOssPath $artifactRoot $ossPath
    Write-Host "Pre-E2E isolation reset completed; removed $removedRedisKeys Redis key(s)."

    $jarPath = Join-Path $repoRoot 'jimuqu-admin\target\jimuqu-admin.jar'
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Backend jar was not produced: $jarPath"
    }
    Assert-PortAvailable $backendPort
    $backendProcess = Start-Process -FilePath $java `
        -ArgumentList @('-jar', $jarPath, '--solon.env=test') `
        -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrorLog `
        -WindowStyle Hidden `
        -PassThru
    Wait-ForBackend $backendProcess "http://127.0.0.1:$backendPort/auth/code" $StartupTimeoutSeconds @($backendOutLog, $backendErrorLog)
    $seedUserCountOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + "--execute=SELECT COUNT(*) FROM $databaseName.sys_user;") $repoRoot (Join-Path $artifactRoot 'mysql-seed-check.log'))
    $seedUserCount = [int](($seedUserCountOutput | Where-Object { $_ -is [string] -and $_ -match '^\d+$' } | Select-Object -Last 1).Trim())
    if ($seedUserCount -ne 7) {
        throw "Fresh E2E database must contain exactly 7 seed users, actual: $seedUserCount"
    }

    Invoke-Checked $corepack @('pnpm', 'install', '--frozen-lockfile') $FrontendDir (Join-Path $artifactRoot 'pnpm-install.log')
    Invoke-Checked $corepack @('pnpm', 'exec', 'playwright', 'install', 'chromium') $FrontendDir (Join-Path $artifactRoot 'playwright-install.log')
    Invoke-Checked $corepack @('pnpm', 'lint') $FrontendDir (Join-Path $artifactRoot 'pnpm-lint.log')
    Invoke-Checked $corepack @('pnpm', 'typecheck') $FrontendDir (Join-Path $artifactRoot 'pnpm-typecheck.log')
    Invoke-Checked $corepack @('pnpm', 'test:unit') $FrontendDir (Join-Path $artifactRoot 'pnpm-unit.log')
    Invoke-Checked $corepack @('pnpm', 'build') $FrontendDir (Join-Path $artifactRoot 'pnpm-build.log')
    $runtimeConfig = Get-ChildItem -LiteralPath (Join-Path $FrontendDir 'dist') -Filter '_app-config-*.js' |
        Select-Object -First 1
    if ($null -eq $runtimeConfig) {
        throw 'Frontend runtime config was not generated.'
    }
    $runtimeConfigText = Get-Content -LiteralPath $runtimeConfig.FullName -Raw
    if (-not $runtimeConfigText.Contains('"VITE_GLOB_API_URL":"/prod-api"')) {
        throw 'Frontend runtime config does not contain the production API prefix: /prod-api'
    }
    if ($runtimeConfigText.Contains("http://127.0.0.1:$backendPort")) {
        throw 'Frontend runtime config leaked the Playwright backend proxy target.'
    }
    if (-not $runtimeConfigText.Contains('"VITE_GLOB_ENABLE_ENCRYPT":"false"')) {
        throw 'Frontend runtime config did not disable transport encryption for the full-stack test.'
    }
    Assert-PortAvailable $frontendPort
    $frontendOutLog = Join-Path $artifactRoot 'frontend.out.log'
    $frontendErrorLog = Join-Path $artifactRoot 'frontend.err.log'
    $frontendProcess = Start-Process -FilePath $corepack `
        -ArgumentList @('pnpm', 'preview', '--host', '127.0.0.1', '--port', "$frontendPort", '--strictPort') `
        -WorkingDirectory $FrontendDir `
        -RedirectStandardOutput $frontendOutLog `
        -RedirectStandardError $frontendErrorLog `
        -WindowStyle Hidden `
        -PassThru
    Wait-ForProcessUrl $frontendProcess "http://127.0.0.1:$frontendPort/" $StartupTimeoutSeconds @($frontendOutLog, $frontendErrorLog)
    $playwrightStarted = $true
    Invoke-Checked $corepack @('pnpm', 'exec', 'playwright', 'test', '--config=playwright.config.ts') $FrontendDir (Join-Path $artifactRoot 'playwright.log')

    Write-Host "Full-stack tests passed. Logs: $artifactRoot"
} catch {
    $runFailure = $_
} finally {
    try {
        if ($null -ne $frontendProcess -and -not $frontendProcess.HasExited) {
            try {
                Stop-Process -Id $frontendProcess.Id -Force -ErrorAction Stop
                $frontendProcess.WaitForExit(10000) | Out-Null
            } catch {
                $cleanupFailures.Add("Frontend process cleanup failed for PID $($frontendProcess.Id): $($_.Exception.Message)")
            }
        }
        if ($null -ne $backendProcess -and -not $backendProcess.HasExited) {
            try {
                Stop-Process -Id $backendProcess.Id -Force -ErrorAction Stop
                $backendProcess.WaitForExit(10000) | Out-Null
            } catch {
                $cleanupFailures.Add("Backend process cleanup failed for PID $($backendProcess.Id): $($_.Exception.Message)")
            }
        }
        if ($null -ne $backendProcess) {
            Start-Sleep -Milliseconds 250
            $backendListeners = @(Get-PortListeners $backendPort)
            if ($backendListeners.Count -gt 0) {
                $owners = ($backendListeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
                $cleanupFailures.Add("Backend port $backendPort is still listening after cleanup; PID(s): $owners")
            }
        }

        if ($playwrightStarted) {
            foreach ($failure in (Stop-TestFrontendListeners $frontendPort $FrontendDir $runStartedAt)) {
                $cleanupFailures.Add($failure)
            }
        }

        if ($redisReady -and $null -ne $redisCli) {
            try {
                Remove-OwnedRedisKeys $redisCli $redisArgs $redisPrefix | Out-Null
            } catch {
                $cleanupFailures.Add("Redis DB${redisDatabase} prefix cleanup failed: $($_.Exception.Message)")
            }
        }

        if ($databaseCreated -and $null -ne $mysql -and $databaseName -match '^jimuqu_it_\d{17}_\d+$') {
            try {
                & $mysql @mysqlArgs "--execute=DROP DATABASE IF EXISTS $databaseName;" | Out-Null
                if ($LASTEXITCODE -ne 0) { throw "mysql exited with $LASTEXITCODE" }
                $databaseCreated = $false
            } catch {
                $cleanupFailures.Add("Database cleanup failed for ${databaseName}: $($_.Exception.Message)")
            }
        } elseif ($databaseCreated) {
            $cleanupFailures.Add("Refusing to drop database with an unexpected generated name: $databaseName")
        }

        if (Test-Path -LiteralPath $ossPath) {
            try {
                $fullArtifactRoot = [IO.Path]::GetFullPath($artifactRoot).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
                $fullOssPath = [IO.Path]::GetFullPath($ossPath)
                if (-not $fullOssPath.StartsWith($fullArtifactRoot, [StringComparison]::OrdinalIgnoreCase)) {
                    throw "Refusing to remove OSS path outside the test artifact directory: $fullOssPath"
                }
                Remove-Item -LiteralPath $fullOssPath -Recurse -Force
                if (Test-Path -LiteralPath $fullOssPath) { throw 'OSS path still exists after removal.' }
            } catch {
                $cleanupFailures.Add("Temporary OSS cleanup failed: $($_.Exception.Message)")
            }
        }
    } finally {
        foreach ($name in $environmentNames) {
            [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
        }
        if ($mutexAcquired) { $runMutex.ReleaseMutex() }
        $runMutex.Dispose()
    }
}

if ($null -ne $runFailure) {
    if ($cleanupFailures.Count -gt 0) {
        Write-Error "Cleanup also failed:`n$($cleanupFailures -join [Environment]::NewLine)" -ErrorAction Continue
    }
    throw $runFailure
}
if ($cleanupFailures.Count -gt 0) {
    throw "Full-stack gates passed, but isolation cleanup failed:`n$($cleanupFailures -join [Environment]::NewLine)"
}
