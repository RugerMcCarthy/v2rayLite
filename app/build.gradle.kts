plugins {
    id("com.android.application")
    id("kotlin-android")
    id ("kotlin-kapt")
    id ("dagger.hilt.android.plugin")
}

val composeVersion: String by extra("1.0.0")
val accompanistVersion: String by extra("0.11.1")
val hilt_version: String by extra("2.37")
val okhttp_version: String by extra("5.0.0-alpha.2")

android{

    signingConfigs {
        create("android_app") {
            keyAlias = "v2ray_lite"
            storeFile = file("/Users/ruger/Tools/v2ray_lite.keystore")
            keyPassword = "gzz3897911"
            storePassword = "gzz3897911"
        }
    }

    compileSdk = 30
    buildToolsVersion ("30.0.3")

    defaultConfig {
        applicationId = "com.thoughtcrime.v2raylite"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.addAll(mutableSetOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("android_app")
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility (JavaVersion.VERSION_1_8)
        targetCompatibility (JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
        kotlinCompilerVersion = "1.5.10"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include ("x86", "x86_64", "armeabi-v7a", "arm64-v8a") //select ABIs to build APKs for
            isUniversalApk = true //generate an additional APK that contains all the ABIs)
        }
    }
}

dependencies {

    // okhttp
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")

    // hilt
    implementation("com.google.dagger:hilt-android:$hilt_version")
    kapt("com.google.dagger:hilt-android-compiler:$hilt_version")

    // accompanist
    implementation("com.google.accompanist:accompanist-insets-ui:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    // Rx
    implementation ("io.reactivex:rxjava:1.3.4")
    implementation ("io.reactivex:rxandroid:1.2.1")
    implementation ("com.tbruyelle.rxpermissions:rxpermissions:0.9.4@aar")

    implementation ("me.drakeet.support:toastcompat:1.1.0")
    implementation ("com.google.code.gson:gson:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation ("com.tencent:mmkv-static:1.2.7")
    implementation ("androidx.core:core-ktx:1.6.0")
    implementation ("androidx.appcompat:appcompat:1.3.0")
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.compose.ui:ui:$composeVersion")
    implementation ("androidx.compose.material:material:$composeVersion")
    implementation ("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation ("androidx.activity:activity-compose:1.3.0-rc02")
    testImplementation ("junit:junit:4.+")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation ("androidx.compose.ui:ui-tooling:$composeVersion")

    implementation(group = "", name="libv2ray", ext = "aar")
}
