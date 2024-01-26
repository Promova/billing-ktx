plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.appsci.billingktx.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.appsci.billingktx.sample"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures { // Enables Jetpack Compose for this module
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation(libs.android.core)
    implementation(libs.android.appcompat)
    implementation(libs.android.compose)

    implementation(libs.google.billing)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    implementation(project(":billingKtx"))
    debugImplementation(libs.compose.tooling)

    implementation(libs.utils.timber)
}
