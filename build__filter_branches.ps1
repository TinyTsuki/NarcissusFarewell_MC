function Split-Patterns([string]$raw) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return @() }
    $seps = [char[]]@(';', '|')
    return $raw.Split($seps, [StringSplitOptions]::RemoveEmptyEntries) | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$wl = Split-Patterns $env:GIT_BRANCH_WHITELIST
$bl = Split-Patterns $env:GIT_BRANCH_BLACKLIST
$useWhitelist = $wl.Count -gt 0

git for-each-ref refs/remotes/origin --format="%(refname:short)" 2>$null | ForEach-Object {
    $short = $_.Trim()
    if ($short -eq 'origin/HEAD') { return }
    $b = $short -replace '^(?i)origin/', ''
    if ([string]::IsNullOrWhiteSpace($b)) { return }

    if ($useWhitelist) {
        foreach ($p in $wl) {
            if ($b -like $p) { Write-Output $b; return }
        }
        return
    }
    foreach ($p in $bl) {
        if ($b -like $p) { return }
    }
    Write-Output $b
}
