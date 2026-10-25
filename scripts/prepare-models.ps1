param([string]$ExistingAssets)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot 'app/src/main/assets'))
$manifest = @{}
Get-Content -LiteralPath (Join-Path $PSScriptRoot 'model-sha256.properties') | ForEach-Object {
    if ($_ -and -not $_.StartsWith('#')) {
        $parts = $_.Split('=', 2)
        if ($parts.Count -ne 2 -or $parts[0] -match '(^[\\/]|\.\.|:)' -or $parts[1] -notmatch '^[a-f0-9]{64}$') { throw 'Invalid model manifest' }
        $manifest[$parts[0]] = $parts[1]
    }
}
function Test-ModelFile([string]$Path, [string]$Hash) {
    (Test-Path -LiteralPath $Path -PathType Leaf) -and ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() -eq $Hash)
}
function Get-Target([string]$Relative) {
    $resolved = [IO.Path]::GetFullPath((Join-Path $assetsRoot $Relative))
    if (-not $resolved.StartsWith($assetsRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Model path left the app asset directory' }
    $resolved
}
if ($ExistingAssets) {
    $sourceRoot = (Resolve-Path -LiteralPath $ExistingAssets).Path
    foreach ($relative in $manifest.Keys) {
        $source = [IO.Path]::GetFullPath((Join-Path $sourceRoot $relative))
        if (-not $source.StartsWith($sourceRoot.TrimEnd('\','/') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid source path' }
        if (-not (Test-ModelFile $source $manifest[$relative])) { throw "Source model does not match: $relative" }
        $target = Get-Target $relative
        if (Test-Path -LiteralPath $target) {
            if (-not (Test-ModelFile $target $manifest[$relative])) { throw "Existing model differs; inspect it before replacing: $target" }
        } else {
            New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
            Copy-Item -LiteralPath $source -Destination $target
        }
    }
} else {
    $downloadRoot = Join-Path $repoRoot '.scratch/model-downloads'
    New-Item -ItemType Directory -Path $downloadRoot -Force | Out-Null
    $whisper = Get-Target 'whisper/ggml-base-q5_1.bin'
    if (-not (Test-ModelFile $whisper $manifest['whisper/ggml-base-q5_1.bin'])) {
        if (Test-Path -LiteralPath $whisper) { throw 'Existing Whisper model differs; inspect before replacing' }
        $download = Join-Path $downloadRoot 'ggml-base-q5_1.bin'
        Invoke-WebRequest -Uri 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin' -OutFile $download
        if (-not (Test-ModelFile $download $manifest['whisper/ggml-base-q5_1.bin'])) { throw 'Whisper checksum mismatch' }
        New-Item -ItemType Directory -Path (Split-Path -Parent $whisper) -Force | Out-Null
        Copy-Item -LiteralPath $download -Destination $whisper
    }
    $voskNeeded = @($manifest.Keys | Where-Object { $_.StartsWith('model-ko/') -and -not (Test-ModelFile (Get-Target $_) $manifest[$_]) })
    if ($voskNeeded.Count) {
        $zipPath = Join-Path $downloadRoot 'vosk-model-small-ko-0.22.zip'
        if (-not (Test-Path -LiteralPath $zipPath)) { Invoke-WebRequest -Uri 'https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip' -OutFile $zipPath }
        $archive = [IO.Compression.ZipFile]::OpenRead($zipPath)
        try {
            foreach ($relative in $voskNeeded) {
                $target = Get-Target $relative
                if (Test-Path -LiteralPath $target) { throw "Existing model differs; inspect before replacing: $target" }
                $entry = $archive.GetEntry('vosk-model-small-ko-0.22/' + $relative.Substring('model-ko/'.Length))
                if (-not $entry) { throw "Vosk archive is missing $relative" }
                New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
                $inputStream = $entry.Open()
                $outputStream = [IO.File]::Create($target)
                try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose(); $inputStream.Dispose() }
                if (-not (Test-ModelFile $target $manifest[$relative])) { throw "Vosk checksum mismatch: $relative" }
            }
        } finally { $archive.Dispose() }
    }
}
foreach ($relative in $manifest.Keys) {
    if (-not (Test-ModelFile (Get-Target $relative) $manifest[$relative])) { throw "Final model verification failed: $relative" }
}
Write-Output "Verified and prepared $($manifest.Count) model files. Model binaries are excluded from Git."
