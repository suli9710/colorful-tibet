# 七彩西藏 · 一键启动脚本
# 用法: 右键 start.bat → 以管理员身份运行，或在 PowerShell 中: .\start.ps1

$ErrorActionPreference = "Stop"
$ProjectDir = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$PreferredWslDistro = "Ubuntu"
$StartupStateDir = Join-Path $ProjectDir ".startup"
$StartupFingerprintFile = Join-Path $StartupStateDir "docker-build-inputs.sha256"

$Script:LastStdOut = ""
$Script:LastStdErr = ""
$Script:WslDistro = $PreferredWslDistro
$Script:WslProjectDir = ""
$Script:WslKeepAliveTag = "colorful-tibet-keepalive"
$Script:ForwardedEnvVars = @(
    "ARK_API_KEY",
    "ARK_API_URL",
    "ARK_STREAM_API_URL",
    "ARK_MODEL",
    "AI_MODEL",
    "AI_STREAM_TIMEOUT",
    "DOUBAO_API_KEY",
    "DOUBAO_API_URL",
    "DOUBAO_STREAM_API_URL",
    "DOUBAO_MODEL"
)

function Normalize-Text {
    param([AllowNull()][string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return ""
    }

    return ($Text -replace "`0", "").Trim()
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $errFile = [System.IO.Path]::GetTempFileName()
    $previousErrorActionPreference = $ErrorActionPreference

    try {
        $ErrorActionPreference = "Continue"
        $output = & $FilePath @Arguments 2>$errFile
        $exitCode = $LASTEXITCODE

        $Script:LastStdOut = Normalize-Text (($output | Out-String))
        $Script:LastStdErr = Normalize-Text (Get-Content -Raw -LiteralPath $errFile -ErrorAction SilentlyContinue)

        return $exitCode -eq 0
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Remove-Item -LiteralPath $errFile -Force -ErrorAction SilentlyContinue
    }
}

function Write-LastNativeError {
    if (-not [string]::IsNullOrWhiteSpace($Script:LastStdErr)) {
        Write-Host $Script:LastStdErr -ForegroundColor Red
    }
    elseif (-not [string]::IsNullOrWhiteSpace($Script:LastStdOut)) {
        Write-Host $Script:LastStdOut -ForegroundColor Red
    }
}

function Get-ForwardedEnvValue {
    param([Parameter(Mandatory = $true)][string]$Name)

    foreach ($target in @(
        [System.EnvironmentVariableTarget]::Process,
        [System.EnvironmentVariableTarget]::User,
        [System.EnvironmentVariableTarget]::Machine
    )) {
        $value = [System.Environment]::GetEnvironmentVariable($Name, $target)
        if (-not [string]::IsNullOrEmpty($value)) {
            return $value
        }
    }

    return $null
}

function ConvertTo-BashSingleQuoted {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) {
        return "''"
    }

    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Get-ForwardedEnvNames {
    return @(
        foreach ($name in $Script:ForwardedEnvVars) {
            if (-not [string]::IsNullOrEmpty((Get-ForwardedEnvValue -Name $name))) {
                $name
            }
        }
    )
}

function Get-WslEnvPrefix {
    $exports = @()
    foreach ($name in $Script:ForwardedEnvVars) {
        $value = Get-ForwardedEnvValue -Name $name
        if (-not [string]::IsNullOrEmpty($value)) {
            $exports += "export $name=$(ConvertTo-BashSingleQuoted -Value $value)"
        }
    }

    if ($exports.Count -eq 0) {
        return ""
    }

    return ($exports -join "; ") + "; "
}

function Wait-BeforeExit {
    if ($env:COLORFUL_TIBET_NO_PAUSE -eq "1") {
        return
    }

    Write-Host "按任意键退出..." -ForegroundColor Gray
    pause
}

function Get-WslDistros {
    if (-not (Invoke-Native -FilePath "wsl.exe" -Arguments @("--list", "--quiet"))) {
        return @()
    }

    return @(
        $Script:LastStdOut -split "\r?\n" |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ -and $_ -notmatch "^docker-desktop" }
    )
}

function Test-WslDistro {
    param([Parameter(Mandatory = $true)][string]$Distro)

    return Invoke-Native -FilePath "wsl.exe" -Arguments @("-d", $Distro, "--", "bash", "-lc", "printf WSL_READY")
}

function Test-WslKeepAlive {
    try {
        $processes = Get-CimInstance Win32_Process -Filter "Name = 'wsl.exe'" -ErrorAction Stop
        return @($processes | Where-Object {
            $_.CommandLine -like "*$Script:WslKeepAliveTag*" -and $_.CommandLine -like "*-d $Script:WslDistro*"
        }).Count -gt 0
    }
    catch {
        return $false
    }
}

function Start-WslKeepAlive {
    if (Test-WslKeepAlive) {
        Write-Host "  WSL keep-alive process already exists." -ForegroundColor Gray
        return
    }

    Write-Host "  Starting WSL keep-alive process..." -ForegroundColor Gray
    $keepAliveArguments = '-d ' + $Script:WslDistro + ' -- bash -lc "exec -a ' + $Script:WslKeepAliveTag + ' tail -f /dev/null"'
    Start-Process `
        -FilePath "wsl.exe" `
        -ArgumentList $keepAliveArguments `
        -WindowStyle Hidden | Out-Null

    Start-Sleep -Seconds 1
}

function Invoke-WslBash {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [switch]$Root
    )

    $arguments = @("-d", $Script:WslDistro)
    if ($Root) {
        $arguments += @("--user", "root")
    }
    $arguments += @("--", "bash", "-lc", "$(Get-WslEnvPrefix)$Command")

    return Invoke-Native -FilePath "wsl.exe" -Arguments $arguments
}

function Get-ProjectCommand {
    param([Parameter(Mandatory = $true)][string]$Command)

    $safeDir = $Script:WslProjectDir.Replace('"', '\"')
    return 'cd "' + $safeDir + '" && ' + $Command
}

function Convert-ToWslPath {
    param([Parameter(Mandatory = $true)][string]$WindowsPath)

    $resolvedPath = (Resolve-Path -LiteralPath $WindowsPath).Path
    if ($resolvedPath -match "^([A-Za-z]):\\(.*)$") {
        $drive = $Matches[1].ToLowerInvariant()
        $path = $Matches[2] -replace "\\", "/"
        return "/mnt/$drive/$path"
    }

    return $resolvedPath
}

function Get-StartupFingerprint {
    $buildInputFiles = @(
        "docker-compose.yml",
        "docker-compose.prod.yml",
        "backend/Dockerfile",
        "backend/pom.xml",
        "frontend/Dockerfile",
        "frontend/nginx.conf",
        "frontend/nginx.http.conf",
        "frontend/package.json",
        "frontend/package-lock.json",
        "frontend/tsconfig.json",
        "frontend/tsconfig.node.json",
        "frontend/vite.config.ts",
        "frontend/tailwind.config.js",
        "scrapler/Dockerfile",
        "scrapler/requirements.txt"
    )
    $buildInputDirectories = @(
        "backend/src",
        "frontend/src",
        "frontend/public",
        "scrapler"
    )
    $excludedPathPattern = "\\(node_modules|dist|target|\.venv|__pycache__|\.pytest_cache|logs)\\"

    $lines = @()
    foreach ($relativePath in $buildInputFiles) {
        $fullPath = Join-Path $ProjectDir $relativePath
        if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
            $hash = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash
            $lines += "$relativePath=$hash"
        }
        else {
            $lines += "$relativePath=missing"
        }
    }

    foreach ($relativeDirectory in $buildInputDirectories) {
        $directory = Join-Path $ProjectDir $relativeDirectory
        if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
            $lines += "$relativeDirectory=missing"
            continue
        }

        Get-ChildItem -LiteralPath $directory -Recurse -File |
            Where-Object { $_.FullName -notmatch $excludedPathPattern } |
            Sort-Object FullName |
            ForEach-Object {
                $relativeFile = $_.FullName.Substring($ProjectDir.Length + 1).Replace("\", "/")
                $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
                $lines += "$relativeFile=$hash"
            }
    }

    return ($lines -join "`n")
}

function Test-StartupFingerprintChanged {
    param([Parameter(Mandatory = $true)][string]$Fingerprint)

    if (-not (Test-Path -LiteralPath $StartupFingerprintFile -PathType Leaf)) {
        return $true
    }

    $previousFingerprint = Get-Content -Raw -LiteralPath $StartupFingerprintFile
    return $previousFingerprint.Trim() -ne $Fingerprint.Trim()
}

function Save-StartupFingerprint {
    param([Parameter(Mandatory = $true)][string]$Fingerprint)

    if (-not (Test-Path -LiteralPath $StartupStateDir -PathType Container)) {
        New-Item -ItemType Directory -Path $StartupStateDir -Force | Out-Null
    }

    Set-Content -LiteralPath $StartupFingerprintFile -Value $Fingerprint -Encoding UTF8
}

function Get-ComposeStatus {
    if (-not (Invoke-WslBash -Command (Get-ProjectCommand -Command "docker compose ps --format json"))) {
        return @()
    }

    $json = $Script:LastStdOut.Trim()
    if ([string]::IsNullOrWhiteSpace($json)) {
        return @()
    }

    try {
        return @($json | ConvertFrom-Json)
    }
    catch {
        $items = @()
        foreach ($line in ($json -split "\r?\n")) {
            $trimmed = $line.Trim()
            if ($trimmed.StartsWith("{")) {
                try {
                    $items += ($trimmed | ConvertFrom-Json)
                }
                catch {
                    # Ignore malformed status lines and continue parsing the rest.
                }
            }
        }
        return $items
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  七彩西藏 Colorful Tibet — 项目启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. 检查 WSL ──
Write-Host "[1/4] 检查 WSL 状态..." -ForegroundColor Yellow

$distros = Get-WslDistros
if ($distros.Count -eq 0) {
    Write-Host "  错误: 未找到可用的 WSL 发行版，请先安装 WSL2 / Ubuntu" -ForegroundColor Red
    Write-LastNativeError
    Wait-BeforeExit
    exit 1
}

if ($distros -contains $PreferredWslDistro) {
    $Script:WslDistro = $PreferredWslDistro
}
else {
    $Script:WslDistro = $distros[0]
    Write-Host "  未找到 $PreferredWslDistro，改用 WSL 发行版: $Script:WslDistro" -ForegroundColor Gray
}

Write-Host "  正在确认 WSL $Script:WslDistro 可用..." -ForegroundColor Gray
if (-not (Test-WslDistro -Distro $Script:WslDistro)) {
    Write-Host "  错误: 无法启动 WSL $Script:WslDistro" -ForegroundColor Red
    Write-LastNativeError
    Wait-BeforeExit
    exit 1
}

$Script:WslProjectDir = Convert-ToWslPath -WindowsPath $ProjectDir

Write-Host "  WSL $Script:WslDistro 就绪" -ForegroundColor Green
Start-WslKeepAlive

$forwardedEnvNames = Get-ForwardedEnvNames
if ($forwardedEnvNames.Count -gt 0) {
    Write-Host "  已转发 AI 环境变量到 WSL/Docker: $($forwardedEnvNames -join ', ')" -ForegroundColor Green
}
else {
    Write-Host "  未检测到 AI 环境变量；AI 路线会使用本地兜底方案" -ForegroundColor Yellow
}

# ── 辅助函数: 等待 Docker 就绪 ──
function Wait-DockerReady {
    for ($retries = 1; $retries -le 15; $retries++) {
        Start-Sleep -Seconds 2
        if (Invoke-WslBash -Command "docker ps >/dev/null") {
            return $true
        }
        Write-Host "  等待 Docker 就绪... ($retries/15)" -ForegroundColor Gray
    }
    return $false
}

# ── 2. 检测 / 启动 Docker Daemon ──
Write-Host "[2/4] 检测 Docker 守护进程..." -ForegroundColor Yellow

$dockerOk = $false
if (Invoke-WslBash -Command "docker ps >/dev/null") { $dockerOk = $true }

if (-not $dockerOk) {
    Write-Host "  Docker 未运行，尝试启动..." -ForegroundColor Gray

    # 方式 1: 通过 service 启动 (--user root 绕过 polkit 认证)
    Invoke-WslBash -Root -Command "service docker start >/dev/null 2>&1" | Out-Null
    Start-Sleep -Seconds 3
    if (Invoke-WslBash -Command "docker ps >/dev/null") { $dockerOk = $true }

    # 方式 2: 直接启动 dockerd
    if (-not $dockerOk) {
        Write-Host "  service 方式失败，直接启动 dockerd..." -ForegroundColor Gray
        Invoke-WslBash -Root -Command "nohup dockerd > /var/log/dockerd.log 2>&1 &" | Out-Null
        $dockerOk = Wait-DockerReady
    }

    if (-not $dockerOk) {
        Write-Host "  错误: Docker 启动失败！" -ForegroundColor Red
        Write-Host "  请手动操作: 打开终端执行 wsl -d Ubuntu，然后 sudo service docker start" -ForegroundColor Red
        Write-LastNativeError
        Wait-BeforeExit
        exit 1
    }
}
Write-Host "  Docker 守护进程就绪" -ForegroundColor Green

# ── 3. 启动项目容器 ──
Write-Host "[3/4] 启动项目容器..." -ForegroundColor Yellow

$startupFingerprint = Get-StartupFingerprint
$startupFingerprintChanged = Test-StartupFingerprintChanged -Fingerprint $startupFingerprint
$forceRebuild = $env:COLORFUL_TIBET_REBUILD -eq "1"
$skipAutoRebuild = $env:COLORFUL_TIBET_SKIP_AUTO_REBUILD -eq "1"

$composeUpCommand = "docker compose up -d"
if ($forceRebuild -or ($startupFingerprintChanged -and -not $skipAutoRebuild)) {
    $composeUpCommand = "docker compose up -d --build"
    if ($forceRebuild) {
        Write-Host "  Forced image rebuild enabled by COLORFUL_TIBET_REBUILD=1." -ForegroundColor Gray
    }
    else {
        Write-Host "  Build inputs changed; rebuilding Docker images automatically." -ForegroundColor Gray
    }
    Write-Host "  已启用强制重建镜像，首次执行可能需要几分钟..." -ForegroundColor Gray
}
else {
    Write-Host "  快速启动现有镜像；如需重建，请先设置 COLORFUL_TIBET_REBUILD=1" -ForegroundColor Gray
}

if (-not (Invoke-WslBash -Command (Get-ProjectCommand -Command $composeUpCommand))) {
    Write-Host "  错误: docker compose up 失败" -ForegroundColor Red
    Write-LastNativeError
    Wait-BeforeExit
    exit 1
}
if (-not [string]::IsNullOrWhiteSpace($Script:LastStdOut)) {
    Write-Host $Script:LastStdOut
}
if (-not ($startupFingerprintChanged -and $skipAutoRebuild -and -not $forceRebuild)) {
    Save-StartupFingerprint -Fingerprint $startupFingerprint
}
Write-Host "  容器已启动，等待健康检查..." -ForegroundColor Gray

# 等待所有容器就绪
$retries = 0
$requiredServices = @("mysql", "redis", "scrapling", "backend", "frontend")
$healthCheckedServices = @("mysql", "redis", "scrapling", "backend")
do {
    Start-Sleep -Seconds 3
    $retries++
    $status = Get-ComposeStatus
    $allHealthy = $true

    foreach ($service in $requiredServices) {
        $entry = @($status | Where-Object { $_.Service -eq $service } | Select-Object -First 1)
        if ($entry.Count -eq 0 -or $entry[0].State -ne "running") {
            $allHealthy = $false
            break
        }
        if ($healthCheckedServices -contains $service -and $entry[0].Health -ne "healthy") {
            $allHealthy = $false
            break
        }
    }

    if ($allHealthy) { break }
    Write-Host "  等待服务就绪... ($retries/20)" -ForegroundColor Gray
} while ($retries -lt 20)

# ── 4. 显示状态 ──
Write-Host "[4/4] 服务状态:" -ForegroundColor Yellow
Write-Host ""

if (Invoke-WslBash -Command (Get-ProjectCommand -Command "docker compose ps")) {
    Write-Host $Script:LastStdOut
}
else {
    Write-LastNativeError
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  项目已启动!" -ForegroundColor Green
Write-Host "  前端:  http://localhost" -ForegroundColor White
Write-Host "  后端:  http://localhost:8080" -ForegroundColor White
Write-Host "  Redis: localhost:6380" -ForegroundColor White
Write-Host "  MySQL: localhost:3307" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 自动打开浏览器
if ($env:COLORFUL_TIBET_NO_BROWSER -ne "1") {
    Start-Process "http://localhost"
}

Wait-BeforeExit
