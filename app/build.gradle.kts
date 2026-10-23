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

// 洹몃┝ ?앹꽦??OpenAI ?? local.properties(源??쒖쇅)?먮쭔 ?먭퀬, ?붾쾭洹?鍮뚮뱶?먮쭔 ?ｋ뒗??
// 由대━??APK??怨듦컻 諛고룷?섎?濡??ㅻ? ?ｌ? ?딅뒗????APK?먯꽌 ?ㅻ? 爰쇰궪 ???덇린 ?뚮Ц.
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
        versionCode = 5
        versionName = "0.2.2"
        buildConfigField("String", "OPENAI_API_KEY", "\"\"")
        buildConfigField("String", "OPENAI_IMAGE_MODEL", "\"$openAiImageModel\"")
        vectorDrawables { useSupportLibrary = true }
        // ?쒖텧??鍮뚮뱶: -PsubmitAbis=arm ?대㈃ x86_64(?먮??덉씠?곗슜)瑜?類??        val arm = listOf("arm64-v8a", "armeabi-v7a")
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

    // 留덉씠洹몃젅?댁뀡 ?뚯뒪?멸? ?대낫???ㅽ궎留?1.json쨌2.json)瑜??쎈뒗??
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
    // room-testing???ㅽ궎留??쎄린媛 kotlinx-serialization 踰꾩쟾 遺덉씪移섎줈 源⑥?吏 ?딅룄濡?留욎텣??PR #1).
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
    // 怨꾩륫 ?뚯뒪??PR #1?먯꽌 ?댁떇): 留덉씠洹몃젅?댁뀡쨌??μ냼 寃利?    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.2")
}
