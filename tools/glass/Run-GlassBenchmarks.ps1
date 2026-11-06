param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][string]$AdbPath,
    [ValidateSet(60, 120)][int]$RefreshRate = 60,
    [string[]]$Methods = @(
        'scrollPhotoLight', 'scrollPhotoDark', 'twentyVisibleMovingWallpaper',
        'outgoingBurst', 'incomingBurst', 'keyboardRoundTrips', 'longPressAndDismiss'
    )
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$outputRoot = Join-Path $projectRoot ('build\glass-perf\' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $RefreshRate)
New-Item -ItemType Directory -Path $outputRoot | Out-Null
$benchmarkClass = 'io.element.android.glassbenchmark.GlassChatBenchmark'
$previousSerial = $env:ANDROID_SERIAL
$env:ANDROID_SERIAL = $Serial

function Save-DeviceSnapshot([string]$Stage, [string]$Directory) {
    & $AdbPath -s $Serial shell getprop ro.product.model | Set-Content -LiteralPath (Join-Path $Directory "$Stage-model.txt")
    & $AdbPath -s $Serial shell getprop ro.build.version.sdk | Set-Content -LiteralPath (Join-Path $Directory "$Stage-sdk.txt")
    & $AdbPath -s $Serial shell dumpsys display | Set-Content -LiteralPath (Join-Path $Directory "$Stage-display.txt")
    & $AdbPath -s $Serial shell dumpsys thermalservice | Set-Content -LiteralPath (Join-Path $Directory "$Stage-thermal.txt")
    & $AdbPath -s $Serial shell dumpsys meminfo io.element.android.glasslab | Set-Content -LiteralPath (Join-Path $Directory "$Stage-memory.txt")
}

try {
    $connection = & $AdbPath -s $Serial get-state
    if ($LASTEXITCODE -ne 0 -or $connection.Trim() -ne 'device') { throw 'The selected physical device must be connected and authorized.' }
    $emulator = & $AdbPath -s $Serial shell getprop ro.kernel.qemu
    if ($emulator.Trim() -eq '1') { throw 'Performance acceptance requires a physical device.' }
    Push-Location -LiteralPath $projectRoot
    try {
        git rev-parse HEAD | Set-Content -LiteralPath (Join-Path $outputRoot 'commit.txt')
        git diff --stat | Set-Content -LiteralPath (Join-Path $outputRoot 'working-tree.txt')
        "requestedRefreshRate=$RefreshRate; measuredIterations=8; compilation=Full; target=glasslab benchmark" |
            Set-Content -LiteralPath (Join-Path $outputRoot 'configuration.txt')
        foreach ($method in $Methods) {
            if ($method -notmatch '^[A-Za-z][A-Za-z0-9]+$') { throw "Invalid method: $method" }
            $methodOutput = Join-Path $outputRoot $method
            New-Item -ItemType Directory -Path $methodOutput | Out-Null
            Save-DeviceSnapshot 'before' $methodOutput
            $started = Get-Date
            & .\gradlew.bat :tests:glassbenchmark:connectedBenchmarkAndroidTest --no-scan `
                "-Pandroid.testInstrumentationRunnerArguments.class=$benchmarkClass#$method" `
                "-Pandroid.testInstrumentationRunnerArguments.refreshRate=$RefreshRate" 2>&1 |
                Tee-Object -FilePath (Join-Path $methodOutput 'gradle.log')
            $gradleExitCode = $LASTEXITCODE
            Save-DeviceSnapshot 'after' $methodOutput
            $outputs = Join-Path $projectRoot 'tests\glassbenchmark\build\outputs'
            if (Test-Path -LiteralPath $outputs) { Copy-Item -LiteralPath $outputs -Destination (Join-Path $methodOutput 'outputs') -Recurse }
            if ($gradleExitCode -ne 0) { throw "Benchmark $method failed; partial evidence is preserved at $methodOutput" }

            $reports = @(Get-ChildItem -LiteralPath (Join-Path $methodOutput 'outputs') -Recurse -Filter '*-benchmarkData.json' |
                Where-Object { $_.LastWriteTime -ge $started.AddSeconds(-2) })
            if ($reports.Count -ne 1) { throw "Expected one fresh JSON report for $method; found $($reports.Count)." }
            $report = Get-Content -LiteralPath $reports[0].FullName -Raw | ConvertFrom-Json
            $entries = @($report.benchmarks)
            if ($entries.Count -ne 1 -or $entries[0].className -ne $benchmarkClass -or $entries[0].name -ne $method) {
                throw 'The report does not match the requested single benchmark.'
            }
            if ($entries[0].repeatIterations -ne 8) { throw 'Expected eight measured iterations; dry-run output is not acceptance evidence.' }
            $traces = @($entries[0].profilerOutputs | Where-Object { $_.type -eq 'PerfettoTrace' })
            if ($traces.Count -ne 8) { throw "Expected eight Perfetto traces, found $($traces.Count)." }
            foreach ($trace in $traces) {
                $tracePath = Join-Path $reports[0].DirectoryName $trace.filename
                if (-not (Test-Path -LiteralPath $tracePath -PathType Leaf)) { throw "Missing trace: $tracePath" }
            }
            Write-Host "Verified ${method}: eight iterations and eight traces archived. Inspect actual display cadence before interpreting $RefreshRate Hz."
        }
    } finally {
        Pop-Location
    }
} finally {
    $env:ANDROID_SERIAL = $previousSerial
}

Write-Host "Evidence: $outputRoot"
