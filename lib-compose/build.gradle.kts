plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.github.jing332.compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    android {
        packaging {
            resources {
                excludes += setOf("META-INF/INDEX.LIST", "META-INF/*.md")
            }
        }
    }
}

dependencies {
    api(project(":lib-common"))

    implementation(libs.bundles.markwon)
    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.coil)

    val composeBom = platform(libs.compose.bom)
//    def composeBom = platform("dev.chrisbanes.compose:compose-bom:2024.01.00-alpha01")
    api(composeBom)
    androidTestApi(composeBom)
    api(libs.bundles.compose)
    api(libs.bundles.compose.material3)

    androidTestApi("androidx.compose.ui:ui-test-junit4")
    debugApi("androidx.compose.ui:ui-test-manifest")
    api("androidx.compose.ui:ui-tooling-preview")
    debugApi("androidx.compose.ui:ui-tooling")

    implementation(libs.coreKtx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}