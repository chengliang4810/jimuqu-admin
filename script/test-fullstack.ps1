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

    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) {
        return $listeners
    }

    # Get-NetTCPConnection can return no rows under heavy Windows resource pressure.
    $netstat = Join-Path $env:SystemRoot 'System32\netstat.exe'
    if (-not (Test-Path -LiteralPath $netstat -PathType Leaf)) {
        return @()
    }
    $portPattern = [regex]::Escape([string]$Port)
    $fallback = foreach ($line in @(& $netstat -ano -p tcp 2>$null)) {
        if ($line -match "^\s*TCP\s+\S+:${portPattern}\s+\S+\s+LISTENING\s+(\d+)\s*$") {
            [pscustomobject]@{ OwningProcess = [int]$Matches[1] }
        }
    }
    return @($fallback)
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

    $baseConfig = Join-Path $RepoRoot 'jimuqu-admin\src\main\resources\app.yml'
    $backendConfig = Join-Path $RepoRoot 'jimuqu-admin\src\main\resources\app-test.yml'
    $commonCacheConfig = Join-Path $RepoRoot 'jimuqu-common\jimuqu-common-cache\src\main\resources\config\common-cache.yml'
    if (-not (Test-Path -LiteralPath $baseConfig -PathType Leaf)) {
        throw "Base configuration is missing: $baseConfig"
    }
    if (-not (Test-Path -LiteralPath $backendConfig -PathType Leaf)) {
        throw "Test configuration is missing: $backendConfig"
    }
    if (-not (Test-Path -LiteralPath $commonCacheConfig -PathType Leaf)) {
        throw "Common cache configuration is missing: $commonCacheConfig"
    }
    $baseText = Get-Content -Raw -LiteralPath $baseConfig
    $requiredBaseCacheMarkers = @(
        '${JIMU_REDIS_SERVER:127.0.0.1:6379}',
        '${JIMU_REDIS_DB:0}',
        '${JIMU_REDIS_PASSWORD:}',
        '${JIMU_REDIS_PREFIX:jimuqu}',
        '${JIMU_SSE_HEARTBEAT_INTERVAL:60000}'
    )
    foreach ($marker in $requiredBaseCacheMarkers) {
        if ($baseText.IndexOf($marker, [StringComparison]::Ordinal) -lt 0) {
            throw "Base cache configuration does not consume required Redis isolation marker ${marker}: $baseConfig"
        }
    }
    $commonCacheText = Get-Content -Raw -LiteralPath $commonCacheConfig
    if ($commonCacheText.IndexOf('keyHeader:', [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        throw "Common cache configuration must not override the application Redis prefix: $commonCacheConfig"
    }
    $backendText = Get-Content -Raw -LiteralPath $backendConfig
    $requiredBackendMarkers = @(
        '${JIMU_TEST_SERVER_PORT}',
        '${JIMU_TEST_MYSQL_DATABASE}',
        '${JIMU_TEST_REDIS_DB}',
        '${JIMU_TEST_REDIS_PASSWORD:}',
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

    $sqlLiteralPattern = '(?i)"\s*(select|insert|update|delete|with)(?:\s+|")'
    $sqlLiteralFindings = Get-ChildItem -LiteralPath $RepoRoot -Recurse -Filter '*.java' -File |
        Where-Object {
            $_.FullName -match '\\src\\main\\java\\' -and
            $_.FullName -notmatch '\\target\\' -and
            $_.FullName -notmatch '\\common\\core\\utils\\sql\\SqlUtil\.java$'
        } |
        Select-String -Pattern $sqlLiteralPattern
    if ($null -ne $sqlLiteralFindings) {
        $summary = ($sqlLiteralFindings | Select-Object -First 20 |
            ForEach-Object { "$($_.Path):$($_.LineNumber)" }) -join ', '
        throw "Application-layer SQL string literals are forbidden. Findings: $summary"
    }
}

function Assert-MavenTestsRan {
    param(
        [string]$RepoRoot,
        [DateTime]$NotBefore,
        [int]$ExpectedHttpOperationCount
    )

    $reports = @(Get-ChildItem -LiteralPath $RepoRoot -Recurse -Filter 'TEST-*.xml' -File |
        Where-Object {
            $_.FullName -match '\\target\\surefire-reports\\' -and
            $_.LastWriteTime -ge $NotBefore.AddSeconds(-2)
        })
    if ($reports.Count -eq 0) {
        throw 'Maven verify completed without any Surefire XML reports; the test gate did not execute tests.'
    }
    $testCount = 0
    $reportBySuite = @{}
    foreach ($report in $reports) {
        [xml]$document = Get-Content -Raw -LiteralPath $report.FullName
        $testCount += [int]$document.testsuite.tests
        $suiteName = [string]$document.testsuite.name
        if (-not [string]::IsNullOrWhiteSpace($suiteName)) {
            $reportBySuite[$suiteName] = [pscustomobject]@{
                Path = $report.FullName
                Document = $document
            }
        }
    }
    if ($testCount -le 0) {
        throw 'Maven verify produced reports but executed zero tests.'
    }

    $requiredSuites = @(
        'com.jimuqu.test.coverage.RuntimeRouteInventoryTest',
        'com.jimuqu.test.http.HttpRouteOwnershipTest',
        'com.jimuqu.test.http.HttpAuthorizationCoverageTest',
        'com.jimuqu.test.http.HealthAuthUserHttpContractTest',
        'com.jimuqu.test.http.RbacHttpContractTest',
        'com.jimuqu.test.http.ResourceMonitorHttpContractTest',
        'com.jimuqu.test.http.ConfigurationMessagingHttpContractTest',
        'com.jimuqu.test.http.PushTransportIntegrationTest'
    )
    $missingSuites = @($requiredSuites | Where-Object { -not $reportBySuite.ContainsKey($_) })
    if ($missingSuites.Count -gt 0) {
        throw "Maven verify did not execute required HTTP coverage suite(s): $($missingSuites -join ', ')"
    }
    foreach ($suiteName in $requiredSuites) {
        $suite = $reportBySuite[$suiteName].Document.testsuite
        if ([int]$suite.tests -le 0 -or [int]$suite.failures -ne 0 -or [int]$suite.errors -ne 0) {
            throw "Required HTTP coverage suite is not green: $suiteName (tests=$($suite.tests), failures=$($suite.failures), errors=$($suite.errors))"
        }
    }

    $inventoryDocument = $reportBySuite['com.jimuqu.test.coverage.RuntimeRouteInventoryTest'].Document
    $inventoryOutput = $inventoryDocument.SelectSingleNode('/testsuite/testcase/system-out')
    if ($null -eq $inventoryOutput) {
        throw 'Runtime route inventory report did not contain its operation-count evidence.'
    }
    $inventoryMatch = [regex]::Match(
        $inventoryOutput.InnerText,
        '(?m)^RUNTIME_ROUTE_INVENTORY_END count=(\d+)\s*$'
    )
    if (-not $inventoryMatch.Success) {
        throw 'Runtime route inventory report did not emit a parseable operation count.'
    }
    $actualHttpOperationCount = [int]$inventoryMatch.Groups[1].Value
    if ($actualHttpOperationCount -ne $ExpectedHttpOperationCount) {
        throw "Runtime HTTP operation count changed: expected=$ExpectedHttpOperationCount actual=$actualHttpOperationCount"
    }
    Write-Host "Maven test gate executed $testCount test(s)."
    Write-Host "Runtime HTTP coverage gate executed all required suites for $actualHttpOperationCount operation(s)."
}

function Assert-PackagedArtifactPolicy {
    param([string]$JarPath)

    if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
        throw "Backend jar was not produced: $JarPath"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $findings = [System.Collections.Generic.List[string]]::new()
        $entryNames = [System.Collections.Generic.HashSet[string]]::new(
            [string[]]@($archive.Entries | ForEach-Object { $_.FullName }),
            [StringComparer]::OrdinalIgnoreCase
        )
        $requiredLegalEntries = @{
            'LICENSE' = @('META-INF/LICENSE', 'BOOT-INF/classes/META-INF/LICENSE')
            'THIRD_PARTY_NOTICES.md' = @(
                'META-INF/THIRD_PARTY_NOTICES.md',
                'BOOT-INF/classes/META-INF/THIRD_PARTY_NOTICES.md'
            )
        }
        foreach ($legalFile in $requiredLegalEntries.GetEnumerator()) {
            $included = @($legalFile.Value | Where-Object { $entryNames.Contains($_) }).Count -gt 0
            if (-not $included) {
                $findings.Add("missing packaged legal notice: $($legalFile.Key)")
            }
        }
        $outerPatterns = @(
            '^BOOT-INF/lib/sa-token-apikey-',
            '^BOOT-INF/lib/jimuqu-(ai|generator|workflow|job)-',
            '^BOOT-INF/classes/com/jimuqu/(ai|generator|workflow|job)/',
            '^BOOT-INF/classes/com/jimuqu/.*/(?:apikey|api_key|plugin)/',
            '^BOOT-INF/classes/com/jimuqu/.*/(?:SysApiKey|SystemMonitor)[^/]*\.class$',
            '^BOOT-INF/classes/.*/(?:mapper|mappers)/.*\.xml$'
        )
        $nestedPatterns = @(
            '^com/jimuqu/(ai|generator|workflow|job)/',
            '^com/jimuqu/.*/(?:apikey|api_key|plugin)/',
            '^com/jimuqu/.*/(?:SysApiKey|SystemMonitor)[^/]*\.class$',
            '(^|/)(?:mapper|mappers)/.*\.xml$'
        )
        $forbiddenClassConstants = @(
            'org/apache/ibatis/annotations/(?:Select|Insert|Update|Delete)(?:Provider)?',
            '(?i)\bselect\s+[^\x00\r\n]{0,300}\s+from\b',
            '(?i)\binsert\s+into\b',
            '(?i)\bupdate\s+[`"A-Za-z0-9_.]+\s+set\b',
            '(?i)\bdelete\s+from\b',
            '(?i)\bwith\s+[`"A-Za-z0-9_]+\s+as\s*\('
        )

        function Test-ClassConstants {
            param(
                [IO.Compression.ZipArchiveEntry]$ClassEntry,
                [string]$DisplayName
            )

            if ($ClassEntry.FullName -notmatch '\.class$' -or
                $ClassEntry.FullName -match '/common/core/utils/sql/SqlUtil\.class$') { return }
            $classStream = $ClassEntry.Open()
            $classBytes = [IO.MemoryStream]::new()
            try {
                $classStream.CopyTo($classBytes)
                $classText = [Text.Encoding]::GetEncoding(28591).GetString($classBytes.ToArray())
                foreach ($pattern in $forbiddenClassConstants) {
                    if ($classText -match $pattern) {
                        $findings.Add("${DisplayName} [forbidden SQL constant]")
                        break
                    }
                }
            } finally {
                $classBytes.Dispose()
                $classStream.Dispose()
            }
        }

        foreach ($entry in $archive.Entries) {
            $entryName = $entry.FullName
            if ($outerPatterns | Where-Object { $entryName -match $_ } | Select-Object -First 1) {
                $findings.Add($entryName)
            }

            if ($entryName -match '^BOOT-INF/classes/com/jimuqu/.+\.class$') {
                Test-ClassConstants $entry $entryName
            }

            if ($entryName -notmatch '^BOOT-INF/lib/jimuqu-[^/]+\.jar$') { continue }

            $entryStream = $entry.Open()
            $memory = [IO.MemoryStream]::new()
            try {
                $entryStream.CopyTo($memory)
                $memory.Position = 0
                $nestedArchive = [IO.Compression.ZipArchive]::new(
                    $memory,
                    [IO.Compression.ZipArchiveMode]::Read,
                    $true
                )
                try {
                    foreach ($nestedEntry in $nestedArchive.Entries) {
                        $nestedName = $nestedEntry.FullName
                        if ($nestedPatterns | Where-Object { $nestedName -match $_ } | Select-Object -First 1) {
                            $findings.Add("${entryName}!/${nestedName}")
                        }
                        if ($nestedName -match '^com/jimuqu/.+\.class$') {
                            Test-ClassConstants $nestedEntry "${entryName}!/${nestedName}"
                        }
                    }
                } finally {
                    $nestedArchive.Dispose()
                }
            } finally {
                $memory.Dispose()
                $entryStream.Dispose()
            }
        }

        if ($findings.Count -gt 0) {
            throw "Packaged jar contains forbidden modules, Mapper SQL, or application SQL constants: $($findings -join ', ')"
        }
    } finally {
        $archive.Dispose()
    }
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

function Assert-NoRedisKeyLeaks {
    param(
        [string]$RedisCli,
        [string[]]$RedisArguments,
        [string[]]$BaselineKeys,
        [string]$Phase
    )

    $baseline = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($key in $BaselineKeys) {
        [void]$baseline.Add($key)
    }
    $leakedKeys = @(Get-RedisKeys $RedisCli $RedisArguments '*' |
        Where-Object { -not $baseline.Contains($_) } |
        Sort-Object -Unique)
    if ($leakedKeys.Count -gt 0) {
        $sample = ($leakedKeys | Select-Object -First 20) -join ', '
        throw "Redis DB15 gained $($leakedKeys.Count) key(s) outside the run-owned prefix during ${Phase}: $sample"
    }
}

function Assert-SeedMenuFrontendContract {
    param(
        [string]$Mysql,
        [string[]]$MysqlArguments,
        [string]$DatabaseName,
        [string]$RepoRoot,
        [string]$FrontendRoot,
        [string]$LogPath
    )

    # mysql --batch escapes control characters embedded inside one CONCAT result.
    # Select independent columns so its tab-delimited output remains directly parseable.
    $sql = "SELECT id, parent_id, HEX(menu_name), COALESCE(component, ''), COALESCE(icon, '') FROM $DatabaseName.sys_menu WHERE menu_type IN ('M','C') ORDER BY id;"
    $output = @(Invoke-CapturedChecked $Mysql ($MysqlArguments + "--execute=$sql") $RepoRoot $LogPath)
    $rows = @($output |
        Where-Object { $_ -is [string] -and $_ -match '^\d+\t\d+\t' } |
        ForEach-Object { ,($_ -split "`t", 5) })
    if ($rows.Count -eq 0) {
        throw 'Fresh E2E database did not expose any directory or page menu rows.'
    }

    $rootMenuNames = @($rows |
        Where-Object { $_[1] -eq '0' } |
        ForEach-Object { $_[2] })
    $expectedRootMenuNames = 'E7B3BBE7BB9FE7AEA1E79086|E7B3BBE7BB9FE79B91E68EA7'
    if (($rootMenuNames -join '|') -ne $expectedRootMenuNames) {
        throw "Backend root menus must be exactly 系统管理 and 系统监控 (UTF-8 hex): $($rootMenuNames -join ', ')"
    }

    $specialComponents = @('Layout', 'ParentView', 'InnerLink')
    $missingComponents = [System.Collections.Generic.List[string]]::new()
    foreach ($row in $rows) {
        $component = $row[3]
        if ([string]::IsNullOrWhiteSpace($component) -or $specialComponents -contains $component) { continue }
        $relativePath = $component.Replace('/', [IO.Path]::DirectorySeparatorChar) + '.vue'
        $componentPath = Join-Path (Join-Path $FrontendRoot 'src\views') $relativePath
        if (-not (Test-Path -LiteralPath $componentPath -PathType Leaf)) {
            $missingComponents.Add("menuId=$($row[0]) -> $component")
        }
    }
    if ($missingComponents.Count -gt 0) {
        throw "Backend menu component(s) do not exist in Bell: $($missingComponents -join ', ')"
    }

    $offlineIconsPath = Join-Path $FrontendRoot 'src\icons\iconify-offline\offline-icons.ts'
    $offlineIconsText = Get-Content -Raw -LiteralPath $offlineIconsPath
    $offlineIcons = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($match in [regex]::Matches($offlineIconsText, "addIcon\(\s*'([^']+)'")) {
        [void]$offlineIcons.Add($match.Groups[1].Value)
    }
    $missingIcons = @($rows |
        ForEach-Object { $_[4] } |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_) -and
            $_ -ne '#' -and
            -not $offlineIcons.Contains($_)
        } |
        Sort-Object -Unique)
    if ($missingIcons.Count -gt 0) {
        throw "Backend menu icon(s) are missing from Bell's offline icon bundle: $($missingIcons -join ', ')"
    }

    Write-Host "Seed menu contract matches Bell: $($rows.Count) directory/page rows, $($offlineIcons.Count) offline icons."
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
        $isRunOwnedNodeFallback = $startedByThisRun -and $null -ne $process -and
            $process.ProcessName.Equals('node', [StringComparison]::OrdinalIgnoreCase)
        if ($startedByThisRun -and (($looksLikePreview -and $belongsToFrontend) -or $isRunOwnedNodeFallback)) {
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
$expectedHttpOperationCount = 137
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
    'JIMU_REDIS_SERVER',
    'JIMU_REDIS_DB',
    'JIMU_REDIS_PASSWORD',
    'JIMU_REDIS_PREFIX',
    'JIMU_SSE_HEARTBEAT_INTERVAL',
    'JIMU_TEST_OSS_DOMAIN',
    'JIMU_TEST_OSS_PATH',
    'JIMU_JUSTAUTH_ENABLED',
    'JIMU_JUSTAUTH_GITEE_CLIENT_ID',
    'JIMU_JUSTAUTH_GITEE_CLIENT_SECRET',
    'JIMU_JUSTAUTH_GITEE_REDIRECT_URI',
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
$databaseCreated = $false
$redisReady = $false
$redisSnapshotTaken = $false
$redisBaselineKeys = @()
$mysql = $null
$redisCli = $null
$mysqlArgs = @()
$redisArgs = @()
$redisDefaultArgs = @()
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
    Write-Host 'Gate order: mapper SQL policy -> Maven clean verify -> backend readiness/Redis write probe -> pnpm install/lint/typecheck/unit/build -> Playwright Chromium.'
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
    $redisDefaultArgs = @('--raw', '-h', $redisHost, '-p', $redisPort, '-n', '0')

    # AutoTable must create the database itself to trigger its database-level seed script.
    $databaseCreated = $true

    $pingOutput = @(Invoke-CapturedChecked $redisCli ($redisArgs + 'PING') $repoRoot (Join-Path $artifactRoot 'redis.log'))
    $ping = ($pingOutput |
        Where-Object { $_ -is [string] -and -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Last 1).Trim()
    if ($ping -ne 'PONG') { throw "Redis DB${redisDatabase} PING returned '$ping' instead of PONG." }
    $redisReady = $true
    $redisBaselineKeys = @(Get-RedisKeys $redisCli $redisArgs '*')
    $redisSnapshotTaken = $true
    $preexistingKeys = @($redisBaselineKeys | Where-Object { $_.StartsWith($redisPrefix, [StringComparison]::Ordinal) })
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
        JIMU_REDIS_SERVER = "${redisHost}:${redisPort}"
        JIMU_REDIS_DB = "$redisDatabase"
        JIMU_REDIS_PASSWORD = $redisPassword
        JIMU_REDIS_PREFIX = $redisPrefix
        JIMU_SSE_HEARTBEAT_INTERVAL = '1000'
        JIMU_TEST_OSS_DOMAIN = "http://127.0.0.1:$backendPort/file/"
        JIMU_TEST_OSS_PATH = $ossPath
        JIMU_JUSTAUTH_ENABLED = 'true'
        JIMU_JUSTAUTH_GITEE_CLIENT_ID = 'http-contract-client'
        JIMU_JUSTAUTH_GITEE_CLIENT_SECRET = 'http-contract-secret'
        JIMU_JUSTAUTH_GITEE_REDIRECT_URI = "http://127.0.0.1:$frontendPort/social-callback?source=gitee"
        PLAYWRIGHT_API_URL = "http://127.0.0.1:$backendPort"
        PLAYWRIGHT_BASE_URL = "http://127.0.0.1:$frontendPort"
        PLAYWRIGHT_REUSE_EXISTING_SERVER = 'true'
        PLAYWRIGHT_WEB_SERVER_COMMAND = "corepack pnpm preview --host 127.0.0.1 --port $frontendPort --strictPort"
        VITE_GLOB_API_URL = '/prod-api'
        VITE_GLOB_ENABLE_ENCRYPT = 'true'
        VITE_PORT = "$frontendPort"
        CI = 'true'
    }
    foreach ($entry in $testEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, [string]$entry.Value, 'Process')
    }

    Assert-NoMapperSql $repoRoot
    $mavenStartedAt = Get-Date
    Invoke-Checked $maven @(
        '-Pdev',
        '-Dsolon.env=test',
        '-DskipTests=false',
        '-DforkCount=0',
        'clean',
        'verify'
    ) $repoRoot (Join-Path $artifactRoot 'maven.log')
    Assert-MavenTestsRan $repoRoot $mavenStartedAt $expectedHttpOperationCount

    $jarPath = Join-Path $repoRoot 'jimuqu-admin\target\jimuqu-admin.jar'
    Assert-PackagedArtifactPolicy $jarPath

    if ($databaseName -notmatch '^jimuqu_it_\d{17}_\d+$') {
        throw "Refusing to reset database with an unexpected generated name: $databaseName"
    }
    Write-Host 'Resetting the Maven-mutated database, Redis namespace and OSS directory before browser E2E.'
    Invoke-Checked $mysql ($mysqlArgs + "--execute=DROP DATABASE $databaseName;") $repoRoot (Join-Path $artifactRoot 'mysql-reset.log')
    $removedRedisKeys = Remove-OwnedRedisKeys $redisCli $redisArgs $redisPrefix
    Assert-NoRedisKeyLeaks $redisCli $redisArgs $redisBaselineKeys 'Maven verification'
    Reset-OwnedOssPath $artifactRoot $ossPath
    Write-Host "Pre-E2E isolation reset completed; removed $removedRedisKeys Redis key(s)."

    Assert-PortAvailable $backendPort
    $backendProcess = Start-Process -FilePath $java `
        -ArgumentList @('-jar', $jarPath, '--solon.env=test') `
        -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrorLog `
        -WindowStyle Hidden `
        -PassThru
    Wait-ForBackend $backendProcess "http://127.0.0.1:$backendPort/auth/code" $StartupTimeoutSeconds @($backendOutLog, $backendErrorLog)
    # Captcha is disabled in the test profile, so readiness itself is intentionally cache-free.
    # Issue a real verification code to force the packaged application through CacheService.store.
    $redisProbeResponse = Invoke-WebRequest `
        -Uri "http://127.0.0.1:$backendPort/resource/sms/code?phoneNumber=13800000002" `
        -UseBasicParsing `
        -TimeoutSec 10
    $redisProbePayload = $redisProbeResponse.Content | ConvertFrom-Json
    if ($redisProbeResponse.StatusCode -ne 200 -or $redisProbePayload.code -ne 200) {
        throw "Backend Redis write probe failed: HTTP=$($redisProbeResponse.StatusCode), code=$($redisProbePayload.code), msg=$($redisProbePayload.msg)"
    }
    $runtimeRedisKeys = @(Get-RedisKeys $redisCli $redisArgs "$redisPrefix*")
    if ($runtimeRedisKeys.Count -eq 0) {
        throw "Backend verification-code probe did not create any run-owned Redis key in DB${redisDatabase}."
    }
    $defaultDatabaseRunKeys = @(Get-RedisKeys $redisCli $redisDefaultArgs "$redisPrefix*")
    if ($defaultDatabaseRunKeys.Count -gt 0) {
        throw "Backend wrote $($defaultDatabaseRunKeys.Count) run-owned Redis key(s) to DB0 instead of DB${redisDatabase}."
    }
    Write-Host "Redis runtime isolation verified: $($runtimeRedisKeys.Count) run-owned key(s) in DB${redisDatabase}, none in DB0."
    $seedUserCountOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + "--execute=SELECT COUNT(*) FROM $databaseName.sys_user;") $repoRoot (Join-Path $artifactRoot 'mysql-seed-check.log'))
    $seedUserCount = [int](($seedUserCountOutput | Where-Object { $_ -is [string] -and $_ -match '^\d+$' } | Select-Object -Last 1).Trim())
    if ($seedUserCount -ne 7) {
        throw "Fresh E2E database must contain exactly 7 seed users, actual: $seedUserCount"
    }
    $forbiddenTableOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + @(
        "--execute=SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$databaseName' AND (TABLE_NAME IN ('sys_job','sys_job_log','sys_plugin','sys_api_key') OR TABLE_NAME REGEXP '^(wf_|flow_|ai_|gen_)');"
    )) $repoRoot (Join-Path $artifactRoot 'mysql-forbidden-table-check.log'))
    $forbiddenTables = @($forbiddenTableOutput |
        Where-Object { $_ -is [string] -and $_ -match '^(sys_(job|job_log|plugin|api_key)|wf_|flow_|ai_|gen_)' } |
        ForEach-Object { $_.Trim() })
    if ($forbiddenTables.Count -gt 0) {
        throw "Fresh E2E database contains excluded module table(s): $($forbiddenTables -join ', ')"
    }
    $primaryKeyOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + @(
        "--execute=SELECT CONCAT(TABLE_NAME, ':', GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '$databaseName' AND INDEX_NAME = 'PRIMARY' AND TABLE_NAME IN ('sys_role_dept','sys_role_menu','sys_user_post','sys_user_role') GROUP BY TABLE_NAME ORDER BY TABLE_NAME;"
    )) $repoRoot (Join-Path $artifactRoot 'mysql-schema-check.log'))
    $actualPrimaryKeys = @($primaryKeyOutput |
        Where-Object { $_ -is [string] -and $_ -match '^sys_[a-z_]+:' } |
        ForEach-Object { $_.Trim() })
    $expectedPrimaryKeys = @(
        'sys_role_dept:role_id,dept_id',
        'sys_role_menu:role_id,menu_id',
        'sys_user_post:user_id,post_id',
        'sys_user_role:user_id,role_id'
    )
    if (($actualPrimaryKeys -join '|') -ne ($expectedPrimaryKeys -join '|')) {
        throw "Fresh E2E database has unexpected RBAC composite primary keys: $($actualPrimaryKeys -join ', ')"
    }

    Assert-SeedMenuFrontendContract $mysql $mysqlArgs $databaseName $repoRoot $FrontendDir `
        (Join-Path $artifactRoot 'mysql-menu-frontend-contract.log')

    $seedCardinalitySql = "SELECT CONCAT((SELECT COUNT(*) FROM $databaseName.sys_user), ':', (SELECT COUNT(*) FROM $databaseName.sys_role), ':', (SELECT COUNT(*) FROM $databaseName.sys_role_dept), ':', (SELECT COUNT(*) FROM $databaseName.sys_menu));"
    $firstSeedCardinalityOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + "--execute=$seedCardinalitySql") $repoRoot (Join-Path $artifactRoot 'mysql-first-start-counts.log'))
    $firstSeedCardinality = ($firstSeedCardinalityOutput |
        Where-Object { $_ -is [string] -and $_ -match '^\d+:\d+:\d+:\d+$' } |
        Select-Object -Last 1).Trim()
    if ($firstSeedCardinality -ne '7:6:1:76') {
        throw "Fresh E2E database has unexpected seed cardinality: $firstSeedCardinality"
    }

    Stop-Process -Id $backendProcess.Id -Force -ErrorAction Stop
    $backendProcess.WaitForExit(10000) | Out-Null
    Start-Sleep -Milliseconds 250
    Assert-PortAvailable $backendPort

    $backendRestartOutLog = Join-Path $artifactRoot 'backend.restart.out.log'
    $backendRestartErrorLog = Join-Path $artifactRoot 'backend.restart.err.log'
    $backendProcess = Start-Process -FilePath $java `
        -ArgumentList @('-jar', $jarPath, '--solon.env=test') `
        -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $backendRestartOutLog `
        -RedirectStandardError $backendRestartErrorLog `
        -WindowStyle Hidden `
        -PassThru
    Wait-ForBackend $backendProcess "http://127.0.0.1:$backendPort/auth/code" $StartupTimeoutSeconds @($backendRestartOutLog, $backendRestartErrorLog)

    $secondSeedCardinalityOutput = @(Invoke-CapturedChecked $mysql ($mysqlArgs + "--execute=$seedCardinalitySql") $repoRoot (Join-Path $artifactRoot 'mysql-second-start-counts.log'))
    $secondSeedCardinality = ($secondSeedCardinalityOutput |
        Where-Object { $_ -is [string] -and $_ -match '^\d+:\d+:\d+:\d+$' } |
        Select-Object -Last 1).Trim()
    if ($secondSeedCardinality -ne $firstSeedCardinality) {
        throw "Second startup changed AutoTable seed cardinality: first=$firstSeedCardinality second=$secondSeedCardinality"
    }
    Write-Host "AutoTable first/second startup seed cardinality is stable: $secondSeedCardinality"

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
    if (-not $runtimeConfigText.Contains('"VITE_GLOB_ENABLE_ENCRYPT":"true"')) {
        throw 'Frontend runtime config did not enable transport encryption for the full-stack test.'
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
        Start-Sleep -Milliseconds 250
        $backendListeners = @(Get-PortListeners $backendPort)
        if ($backendListeners.Count -gt 0) {
            $owners = ($backendListeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
            $cleanupFailures.Add("Backend port $backendPort is still listening after cleanup; PID(s): $owners")
        }

        foreach ($failure in (Stop-TestFrontendListeners $frontendPort $FrontendDir $runStartedAt)) {
            $cleanupFailures.Add($failure)
        }

        if ($redisReady -and $null -ne $redisCli) {
            try {
                Remove-OwnedRedisKeys $redisCli $redisArgs $redisPrefix | Out-Null
                if ($redisDefaultArgs.Count -gt 0) {
                    Remove-OwnedRedisKeys $redisCli $redisDefaultArgs $redisPrefix | Out-Null
                }
                if ($redisSnapshotTaken) {
                    Assert-NoRedisKeyLeaks $redisCli $redisArgs $redisBaselineKeys 'full-stack verification'
                }
            } catch {
                $cleanupFailures.Add("Redis DB${redisDatabase} isolation cleanup failed: $($_.Exception.Message)")
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
