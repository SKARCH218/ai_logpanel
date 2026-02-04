import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Google Generative AI KMP SDK
            implementation("dev.shreyaspatil.generativeai:generativeai-google:0.9.0-1.1.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("com.jcraft:jsch:0.1.55")
            implementation("org.yaml:snakeyaml:2.2")
            implementation("org.commonmark:commonmark:0.22.0")
            // 기존 잘못된 의존성 제거
            // implementation("com.google.ai.client:generativeai:0.3.0")
        }
    }
}


compose.desktop {
    application {
        mainClass = "dev.skarch.ai_logpanel.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "AI-Log-Panel"
            packageVersion = "1.0.0"
            description = "AI-powered server log management tool"
            copyright = "© 2024 SKARCH. All rights reserved."
            vendor = "SKARCH"

            windows {
                // Windows 전용 설정 - ICO 파일 사용
                iconFile.set(project.file("src/jvmMain/resources/logo.ico"))
                menuGroup = "AI-Log-Panel"
                perUserInstall = true

                // 바로가기 생성
                shortcut = true
                dirChooser = true

                // 실행 파일 이름
                exePackageVersion = packageVersion
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/logo.png"))
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/logo.png"))
            }
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}
