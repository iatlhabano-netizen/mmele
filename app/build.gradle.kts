plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.aboutlibraries)
}

// Online play is optional: the Google Services plugin only runs when the developer has dropped a
// google-services.json from their own Firebase project into app/. Without it the app builds and
// shows "Online play not configured yet". See docs/ONLINE_SETUP.md.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.tshipiworks.mmele"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tshipiworks.mmele"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }

    // Upload key lives outside the repo; its path and passwords come from ~/.gradle/gradle.properties
    // (MMELE_UPLOAD_*). Without them the release build is simply unsigned.
    val uploadStore = providers.gradleProperty("MMELE_UPLOAD_STORE_FILE").orNull
    signingConfigs {
        if (uploadStore != null) {
            create("release") {
                storeFile = file(uploadStore)
                storePassword = providers.gradleProperty("MMELE_UPLOAD_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("MMELE_UPLOAD_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("MMELE_UPLOAD_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (uploadStore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":online"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.aboutlibraries.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Open-source licence list for the About screen (generated from the dependency graph at build time).
aboutLibraries {
    registerAndroidTasks = true
    excludeFields = arrayOf("generated")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
