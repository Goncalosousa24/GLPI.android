plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.glpimobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.glpimobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 🔥 ESTA É A PARTE MAIS IMPORTANTE PARA O TEU KOTLIN FUNCIONAR 🔥
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
// --- 🏗️ CORE & UI BASE ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

    // 🔥 ESTA LINHA RESOLVE O ERRO DO XML (ConstraintLayout)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- 🎨 MATERIAL DESIGN & ANIMAÇÕES ---
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.airbnb.android:lottie:6.4.0")

    // --- 🌐 NETWORKING (RETROFIT + OKHTTP) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- ⚙️ COROUTINES & GESTÃO DE DADOS ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- 📊 GRÁFICOS (MPAndroidChart) ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- 📷 CAMERA X (SCAN DE INVENTÁRIO) ---
    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // --- 🤖 ML KIT (BARCODE & OCR) ---
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // --- 🔨 WORK MANAGER (NOTIFICAÇÕES 15 MIN) ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- 🧪 TESTES ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}