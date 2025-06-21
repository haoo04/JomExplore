plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.jomexplore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jomexplore"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // CameraX core library
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Guava
    implementation(libs.guava)

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // For reading EXIF data from images
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ARCore (Google Play Services for AR) - for future AR implementation
    implementation("com.google.ar:core:1.49.0")

    // Filament for 3D model rendering
    implementation("com.google.android.filament:filament-android:1.31.5")
    implementation("com.google.android.filament:gltfio-android:1.31.5")
    implementation("com.google.android.filament:filament-utils-android:1.31.5")
}