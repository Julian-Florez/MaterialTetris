import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                // Añadir Material3 y UI para el módulo desktop
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
    }
}

compose.desktop {
    application {
        // Updated to fully qualified name after moving Main.kt to a package
        mainClass = "com.myg.materialtetris.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Tetris"
            packageVersion = "1.0.0"
            
            // Set vendor name for Windows installer metadata
            vendor = "MaterialYou Games"
            
            // Fixes "Failed to launch JVM" by including all JRE modules
            includeAllModules = true
            
            windows {
                console = false // Disable console
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                shortcut = true // Create desktop shortcut
            }
            
            linux {
                iconFile.set(project.file("../app/src/main/ic_launcher-playstore.png"))
            }
            
            macOS {
                iconFile.set(project.file("../app/src/main/ic_launcher-playstore.png")) // .icns is standard but PNG might work or fail gracefully
            }
        }
    }
}
