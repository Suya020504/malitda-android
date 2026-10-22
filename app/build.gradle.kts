import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// 그림 생성용 OpenAI 키. local.properties(깃 제외)에만 두고, 디버그 빌드에만 넣는다.
// 릴리스 APK는 공개 배포되므로 키를 넣지 않는다 — APK에서 키를 꺼낼 수 있기 때문.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val openAiKey: String = localProps.getProperty("OPENAI_API_KEY", "").trim()
val openAiImageModel: String = localProps.getProperty("OPENAI_IMAGE_MODEL", "gpt-image-1").trim()

android {
    namespace = "kr.voicemate.malitda"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.voicemate.malitda"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 4
        versionName = "0.2.1"
        buildConfigField("String", "OPENAI_API_KEY", "\"\"")
        buildConfigField("String", "OPENAI_IMAGE_MODEL", "\"$openAiImageModel\"")
        vectorDrawables { useSupportLibrary = true }
        // 제출용 빌드: -PsubmitAbis=arm 이면 x86_64(에뮬레이터용)를 뺀다
        val arm = listOf("arm64-v8a", "armeabi-v7a")
        ndk { abiFilters += if (project.findProperty("submitAbis") == "arm") arm else arm + "x86_64" }
        externalNativeBuild {
            cmake {
                val whisperDir = (project.findProperty("whisperCppDir") as String?) ?: rootProject.file("third_party/whisper.cpp").absolutePath
                arguments += listOf("-DWHISPER_CPP_DIR=${whisperDir.replace('\\', '/')}", "-DANDROID_STL=c++_static")
                cppFlags += listOf("-O2")
            }
        }
    }

    ndkVersion = "27.2.12479018"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
        }
    }

    // 마이그레이션 테스트가 내보낸 스키마(1.json·2.json)를 읽는다.
    sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/DEPENDENCIES")
        jniLibs.useLegacyPackaging = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    // room-testing의 스키마 읽기가 kotlinx-serialization 버전 불일치로 깨지지 않도록 맞춘다(PR #1).
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1"))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.vosk.android)
    implementation("${libs.jna.get()}@aar")
    implementation(libs.sqlcipher.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    // 계측 테스트(PR #1에서 이식): 마이그레이션·저장소 검증
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.2")
}
