plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.github.jing332.script_engine"
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
    composeOptions{
        kotlinCompilerExtensionVersion = libs.versions.composeComplile.get()
    }

    packaging {
        resources {
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/*.md")
        }
    }
}

dependencies {
//    api(fileTree("include" to listOf("*.jar"), "dir" to "libs"))
//    implementation("io.apisense:rhino-android:1.3.0")

    api(project(":lib-common"))
    api(project(":lib-compose"))

    implementation(libs.hutool.crypto)
    implementation(libs.bundles.network)

    implementation(libs.bundles.compose)
    implementation(libs.coreKtx)
    implementation(libs.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}