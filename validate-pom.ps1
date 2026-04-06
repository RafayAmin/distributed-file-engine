$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot
mvn help:effective-pom -q 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host 'POM is valid'
    exit 0
}
Write-Host 'POM is invalid'
exit $LASTEXITCODE
