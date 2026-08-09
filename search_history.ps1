$allHashes = git log --all --format="%H" 2>$null

foreach ($h in $allHashes) {
    $content = git show "${h}:app/src/main/java/com/example/app/AbacusCustomizationFragment.kt" 2>$null
    if ($content) {
        $found = $content | Select-String 'overlayFrameBg'
        if ($found) {
            Write-Output "=== FOUND IN: $h ==="
            $found | Select-Object -First 5 | ForEach-Object { Write-Output $_.Line }
            break
        }
    }
}
