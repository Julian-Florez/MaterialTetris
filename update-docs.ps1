# Script para actualizar la carpeta docs con la versión de producción
# Ejecutar después de compilar: .\gradlew :web:wasmJsBrowserDistribution

$sourcePath = ".\web\build\dist\wasmJs\productionExecutable"
$destPath = ".\docs"

# Guardar colors.txt existente
$colorsBackup = $null
if (Test-Path "$destPath\colors.txt") {
    $colorsBackup = Get-Content "$destPath\colors.txt" -Raw
}

# Limpiar carpeta docs
if (Test-Path $destPath) {
    Remove-Item -Path "$destPath\*" -Recurse -Force
}

# Copiar nuevos archivos
Copy-Item -Path "$sourcePath\*" -Destination $destPath -Recurse -Force

# Restaurar colors.txt si existía
if ($colorsBackup) {
    Set-Content -Path "$destPath\colors.txt" -Value $colorsBackup -NoNewline
    Write-Host "colors.txt restaurado" -ForegroundColor Cyan
}

Write-Host "Archivos copiados a la carpeta docs:" -ForegroundColor Green
Get-ChildItem $destPath

Write-Host ""
Write-Host "Para publicar en GitHub Pages:" -ForegroundColor Yellow
Write-Host "1. git add docs"
Write-Host "2. git commit -m 'Update GitHub Pages'"
Write-Host "3. git push"

