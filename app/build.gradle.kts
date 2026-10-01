plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.aimetadatacleaner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.aimetadatacleaner.cleaner.gpljxi"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation("junit:junit:4.13.2")
}

tasks.register("importUploadedQr") {
    doLast {
        val searchDirs = listOf(rootDir, projectDir, file("${projectDir}/src/main/res/drawable"))
        val candidateNames = listOf("QR.png", "qr.png", "QR.jpg", "qr.jpg", "QR.jpeg", "qr.jpeg", "donate_qr.jpg", "donate_qr.jpeg")
        val target = file("${projectDir}/src/main/res/drawable/donate_qr.png")
        for (dir in searchDirs) {
            for (name in candidateNames) {
                val candidate = file("$dir/$name")
                if (candidate.exists() && candidate.absolutePath != target.absolutePath) {
                    println("Automatically imported uploaded QR: ${candidate.name} -> donate_qr.png")
                    candidate.copyTo(target, overwrite = true)
                    candidate.delete()
                    return@doLast
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("pre") || it.name.startsWith("merge") }.configureEach {
    dependsOn("importUploadedQr")
}

