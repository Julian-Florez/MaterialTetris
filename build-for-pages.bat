@echo off
echo Compilando Material Snake para web...
call gradlew.bat :web:wasmJsBrowserDistribution

echo Copiando archivos a docs...
copy /Y "web\build\kotlin-webpack\wasmJs\productionExecutable\*.js" "docs\"
copy /Y "web\build\kotlin-webpack\wasmJs\productionExecutable\*.wasm" "docs\"
copy /Y "web\src\wasmJsMain\resources\colors.txt" "docs\"

echo.
echo ¡Listo! Los archivos han sido copiados a la carpeta docs.
echo Ahora puedes hacer commit y push a GitHub.
pause

