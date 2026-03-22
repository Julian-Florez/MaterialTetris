# Material Tetris 🧱

Un juego de Tetris moderno con Material Design 3, construido con Kotlin Multiplatform y Compose.

## 🎮 Jugar Online

[**¡Juega ahora en tu navegador!**](https://julian-florez.github.io/MaterialTetris/)


## 🚀 Plataformas

- **Web (WebAssembly)** - Juega directamente en el navegador
- **Desktop** - Windows, macOS, Linux
- **Android**

## 🎯 Controles

- **Flechas del teclado** o **WASD** - Mover
- **R** - Reiniciar el juego
- **Pantalla táctil** - Desliza para mover (en móviles)

## 🛠️ Tecnologías

- Kotlin Multiplatform
- Jetpack Compose / Compose Multiplatform
- Material Design 3
- WebAssembly (Kotlin/Wasm)

## 📦 Compilar

### Web

```bash
./gradlew :web:wasmJsBrowserDevelopmentRun
```

### Desktop

```bash
./gradlew :desktop:run
```

### Android

Ejecutar en un dispositivo o emulador con la build de debug:

```bash
./gradlew :app:installDebug
```

Generar el APK listo para distribuir (carpeta `app/build/outputs/apk/release/`):

```bash
./gradlew :app:assembleRelease
```

### Instalador Windows (MSI)

```bash
./gradlew :desktop:packageReleaseMsi
```

### Producción Web

```bash
./gradlew :web:wasmJsBrowserDistribution
```
