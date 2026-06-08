Set-StrictMode -Version 2.0

$script:DeploymentArchiveForbiddenPathPatterns = @(
    '^(?:output|data|logs|certs|backend/uploads|backend/target|frontend/dist|scrapler/\.venv|scrapler/__pycache__)(?:/|$)',
    '(?:^|/)node_modules(?:/|$)',
    '(?:^|/)__pycache__(?:/|$)',
    '\.(?:pyc|pyo|log)$',
    '\.(?:tar|tgz|gz|zip|7z|rar)$',
    '^"[^/]*$',
    '^''[^/]*$',
    '^"?%s[^/]*$',
    '^[^/]*;[^/]*$',
    '(?i)(?:^|/)[^/]*(?:totp|2fa|two-factor)[^/]*\.(?:png|jpg|jpeg|webp|gif|svg)$'
)

$script:DeploymentArchiveExcludedPathPatterns = @(
    '^\.startup(?:/|$)'
)

$script:DeploymentArchiveGitExcludePathspecs = @(
    ':(exclude).startup'
)

$script:DeploymentArchiveUntrackedBlockingPathRules = @(
    @{
        Pattern = '^backend/src(?:/|$)'
        Reason = 'backend source under backend/src'
    },
    @{
        Pattern = '^backend/(?:pom\.xml|mvnw(?:\.cmd)?|\.mvn/|Dockerfile(?:\..*)?)(?:/|$)'
        Reason = 'backend build or Docker file'
    },
    @{
        Pattern = '^frontend/src(?:/|$)'
        Reason = 'frontend source under frontend/src'
    },
    @{
        Pattern = '^frontend/(?:public/|index\.html|package(?:-lock)?\.json|vite\.config\.[^/]+|tsconfig(?:\.[^/]+)?\.json|tailwind\.config\.[^/]+|postcss\.config\.[^/]+|Dockerfile(?:\..*)?|nginx(?:\.[^/]+)?\.conf)(?:/|$)'
        Reason = 'frontend source, build, nginx, or Docker file'
    },
    @{
        Pattern = '^scrapler/(?:Dockerfile(?:\..*)?|requirements(?:-[^/]+)?\.txt|[^/]+\.(?:py|toml|ya?ml)|src/|app/)(?:/|$)'
        Reason = 'scrapler service source or build file'
    },
    @{
        Pattern = '^scripts(?:/|$)'
        Reason = 'deployment script under scripts'
    },
    @{
        Pattern = '^(?:docker-compose(?:\.[^/]+)?\.ya?ml|compose(?:\.[^/]+)?\.ya?ml)$'
        Reason = 'Docker Compose file'
    },
    @{
        Pattern = '^(?:Dockerfile(?:\..*)?|\.dockerignore|\.env\.example)$'
        Reason = 'root deployment or environment template file'
    },
    @{
        Pattern = '^[^/]*(?:deploy|deployment|upload-server|server-http)[^/]*\.(?:ps1|sh|bash|cmd|bat)$'
        Reason = 'root deployment/upload script'
    },
    @{
        Pattern = '^(?:monitoring|nginx|certbot)(?:/|$)'
        Reason = 'deployment support configuration'
    }
)

function ConvertTo-DeploymentArchivePath {
    param([string]$Path)
    return (($Path -replace '\\', '/') -replace '^\./', '')
}

function Test-DeploymentArchiveAsciiPath {
    param([string]$Path)
    return [regex]::IsMatch($Path, '^[\u0000-\u007F]+$')
}

function Get-DeploymentArchiveForbiddenReason {
    param([string]$Path)

    $normalized = ConvertTo-DeploymentArchivePath $Path

    if ($normalized -match '(?:^|/)\.env(?:$|\.)' -and $normalized -notmatch '(?:^|/)\.env\.example$') {
        return "environment file"
    }

    foreach ($pattern in $script:DeploymentArchiveForbiddenPathPatterns) {
        if ($normalized -match $pattern) {
            return "forbidden deployment artifact pattern: $pattern"
        }
    }

    return $null
}

function Get-DeploymentArchiveUntrackedBlockingReason {
    param([string]$Path)

    $normalized = ConvertTo-DeploymentArchivePath $Path
    foreach ($rule in $script:DeploymentArchiveUntrackedBlockingPathRules) {
        if ($normalized -match $rule["Pattern"]) {
            return $rule["Reason"]
        }
    }

    return $null
}

function Get-DeploymentArchiveUntrackedFiles {
    param([string]$RepoRoot)

    $untrackedFileOutput = & git -C $RepoRoot ls-files --others --exclude-standard -z
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to list untracked files before creating the deployment archive."
    }

    if (-not $untrackedFileOutput) {
        return @()
    }

    return @($untrackedFileOutput -split "`0" | ForEach-Object { ConvertTo-DeploymentArchivePath $_ } | Where-Object { $_ } | Sort-Object -Unique)
}

function Assert-NoDeploymentArchiveUntrackedProductionFiles {
    param([string]$RepoRoot)

    $untrackedFiles = Get-DeploymentArchiveUntrackedFiles $RepoRoot
    $blockedUntrackedFiles = @()
    foreach ($relativePath in $untrackedFiles) {
        $reason = Get-DeploymentArchiveUntrackedBlockingReason $relativePath
        if ($reason) {
            $blockedUntrackedFiles += "$relativePath ($reason)"
        }
    }

    if ($blockedUntrackedFiles.Count -gt 0) {
        throw "Refusing to create deployment archive because untracked production/deployment files would be omitted from the Git-tree archive:`n$($blockedUntrackedFiles -join "`n")`nAdd these files to Git, remove them from production/deployment paths, or explicitly handle them before deploying."
    }
}

function Get-DeploymentArchiveExcludedReason {
    param([string]$Path)

    $normalized = ConvertTo-DeploymentArchivePath $Path
    foreach ($pattern in $script:DeploymentArchiveExcludedPathPatterns) {
        if ($normalized -match $pattern) {
            return "excluded deployment-only local artifact pattern: $pattern"
        }
    }

    return $null
}

function Assert-DeploymentArchiveContents {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ArchivePath,

        [Parameter(Mandatory = $true)]
        [string[]]$ExpectedEntries
    )

    $previousConsoleOutputEncoding = [Console]::OutputEncoding
    $previousOutputEncoding = $OutputEncoding
    try {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [Console]::OutputEncoding = $utf8NoBom
        $OutputEncoding = $utf8NoBom
        $entries = & tar -tzf $ArchivePath
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to inspect deployment archive: $ArchivePath"
        }
    } finally {
        [Console]::OutputEncoding = $previousConsoleOutputEncoding
        $OutputEncoding = $previousOutputEncoding
    }

    $normalizedEntries = @(
        $entries |
            ForEach-Object { ConvertTo-DeploymentArchivePath $_ } |
            Where-Object { $_ -and -not $_.EndsWith("/") } |
            Sort-Object -Unique
    )
    if (-not $normalizedEntries) {
        throw "Deployment archive is empty."
    }

    $expectedNormalizedEntries = @(
        $ExpectedEntries |
            ForEach-Object { ConvertTo-DeploymentArchivePath $_ } |
            Where-Object { $_ -and -not $_.EndsWith("/") } |
            Sort-Object -Unique
    )
    $expectedAsciiEntries = @($expectedNormalizedEntries | Where-Object { Test-DeploymentArchiveAsciiPath $_ })
    $actualAsciiEntries = @($normalizedEntries | Where-Object { Test-DeploymentArchiveAsciiPath $_ })
    $missingEntries = @($expectedAsciiEntries | Where-Object { $actualAsciiEntries -notcontains $_ })
    $unexpectedEntries = @($actualAsciiEntries | Where-Object { $expectedAsciiEntries -notcontains $_ })
    $entryCountMismatch = $expectedNormalizedEntries.Count -ne $normalizedEntries.Count
    if ($missingEntries.Count -gt 0 -or $unexpectedEntries.Count -gt 0) {
        $details = @()
        if ($entryCountMismatch) {
            $details += "Expected entry count: $($expectedNormalizedEntries.Count); actual entry count: $($normalizedEntries.Count)"
        }
        if ($missingEntries.Count -gt 0) {
            $details += "Missing expected ASCII entries:"
            $details += $missingEntries
        }
        if ($unexpectedEntries.Count -gt 0) {
            $details += "Unexpected ASCII archive entries:"
            $details += $unexpectedEntries
        }
        throw "Deployment archive contents do not match the approved Git-tree file list:`n$($details -join "`n")"
    }
    if ($entryCountMismatch) {
        throw "Deployment archive entry count does not match the approved Git-tree file list: expected $($expectedNormalizedEntries.Count), actual $($normalizedEntries.Count)."
    }

    $forbiddenEntries = @()
    foreach ($entry in $normalizedEntries) {
        $reason = Get-DeploymentArchiveForbiddenReason $entry
        if ($reason) {
            $forbiddenEntries += "$entry ($reason)"
        }
    }

    if ($forbiddenEntries.Count -gt 0) {
        throw "Deployment archive contains forbidden local/generated/PII-risk paths:`n$($forbiddenEntries -join "`n")"
    }

    Write-Host "Archive safety check passed: $($ExpectedEntries.Count) Git-tree files approved; tar contents contain no forbidden local uploads/generated artifacts." -ForegroundColor Green
}

function New-DeploymentArchive {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,

        [Parameter(Mandatory = $true)]
        [string]$ArchivePath
    )

    foreach ($command in @("git", "tar")) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
            throw "Missing required command: $command"
        }
    }

    $resolvedRepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
    $gitTopLevel = (& git -C $resolvedRepoRoot rev-parse --show-toplevel).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $gitTopLevel) {
        throw "Unable to resolve Git repository root for $resolvedRepoRoot"
    }

    $resolvedGitTopLevel = (Resolve-Path -LiteralPath $gitTopLevel).Path
    if ($resolvedGitTopLevel -ne $resolvedRepoRoot) {
        throw "Refusing to archive from $resolvedRepoRoot because Git root is $resolvedGitTopLevel"
    }

    Assert-NoDeploymentArchiveUntrackedProductionFiles $resolvedRepoRoot

    $archiveTree = (& git -C $resolvedRepoRoot stash create "deployment archive")
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create a temporary Git tree for the deployment archive."
    }

    $archiveTree = "$archiveTree".Trim()
    if (-not $archiveTree) {
        $archiveTree = "HEAD"
    }

    $trackedFileOutput = & git -C $resolvedRepoRoot ls-tree -r --name-only -z $archiveTree
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to list files in Git tree $archiveTree."
    }

    $trackedFiles = @($trackedFileOutput -split "`0" | ForEach-Object { ConvertTo-DeploymentArchivePath $_ } | Where-Object { $_ } | Sort-Object -Unique)
    if (-not $trackedFiles) {
        throw "No Git-tracked files found to upload from tree $archiveTree."
    }

    $blockedTrackedFiles = @()
    $archiveFiles = @()
    foreach ($relativePath in $trackedFiles) {
        if (Get-DeploymentArchiveExcludedReason $relativePath) {
            continue
        }

        $reason = Get-DeploymentArchiveForbiddenReason $relativePath
        if ($reason) {
            $blockedTrackedFiles += "$relativePath ($reason)"
            continue
        }

        $archiveFiles += $relativePath
    }

    if ($blockedTrackedFiles.Count -gt 0) {
        throw "Refusing to create deployment archive because Git tracks forbidden local/generated/PII-risk paths:`n$($blockedTrackedFiles -join "`n")"
    }

    if (-not $archiveFiles) {
        throw "No files remain after deployment archive filtering."
    }

    $archiveDirectory = Split-Path -Parent $ArchivePath
    if ($archiveDirectory -and -not (Test-Path -LiteralPath $archiveDirectory)) {
        New-Item -ItemType Directory -Path $archiveDirectory | Out-Null
    }

    & git -C $resolvedRepoRoot archive --format=tar.gz "--output=$ArchivePath" $archiveTree -- . $script:DeploymentArchiveGitExcludePathspecs
    if ($LASTEXITCODE -ne 0) {
        throw "git archive failed with exit code $LASTEXITCODE"
    }

    Assert-DeploymentArchiveContents -ArchivePath $ArchivePath -ExpectedEntries $archiveFiles
}
