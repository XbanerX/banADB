plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.baner.banadb"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.baner.banadb"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keyFile = file("banadb-release.jks")
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = "banadb2026"
                keyAlias = "banadb"
                keyPassword = "banadb2026"
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/**"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs(layout.buildDirectory.dir("generated/assets/clip"))
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // KADB - pure Kotlin ADB protocol
    implementation("com.flyfishxu:kadb:2.1.4")
    implementation("com.flyfishxu:kadb-mdns:2.1.4")

    // Okio
    implementation("com.squareup.okio:okio:3.9.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.13.1")
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

val compileClipRunner = tasks.register<JavaCompile>("compileClipRunner") {
    source = fileTree("src/main/clip")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/clip_classes"))
    classpath = files(android.bootClasspath)
    options.release.set(8)
}

val buildClipJar = tasks.register("buildClipJar") {
    dependsOn(compileClipRunner)
    val inputClassDir = compileClipRunner.flatMap { it.destinationDirectory }
    val outputDir = layout.buildDirectory.dir("generated/assets/clip")
    val outputJar = outputDir.map { it.file("banaclip.jar") }

    inputs.dir(inputClassDir)
    outputs.file(outputJar)

    doLast {
        val outJarFile = outputJar.get().asFile
        outJarFile.parentFile.mkdirs()

        val sdkDir = android.sdkDirectory
        val buildToolsVersion = android.buildToolsVersion
        val d8Candidates = listOf(
            File(sdkDir, "build-tools/$buildToolsVersion/d8"),
            File(sdkDir, "build-tools").listFiles()?.sortedDescending()?.map { File(it, "d8") }?.firstOrNull { it.exists() }
        ).filterNotNull().filter { it.exists() }

        val d8Path = d8Candidates.firstOrNull()?.absolutePath ?: "d8"
        val classFiles = fileTree(inputClassDir.get().asFile).matching { include("**/*.class") }.files

        project.exec {
            commandLine(d8Path, "--output", outJarFile.absolutePath, *classFiles.map { it.absolutePath }.toTypedArray())
        }
    }
}

tasks.named("preBuild") {
    dependsOn(buildClipJar)
}

tasks.matching { it.name.contains("Lint") }.configureEach {
    dependsOn(buildClipJar)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(buildClipJar)
}

