plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val vMajor = providers.gradleProperty("BUPAS_VERSION_MAJOR").orElse("1").get()
val vMinor = providers.gradleProperty("BUPAS_VERSION_MINOR").orElse("0").get()
val builderTag = providers.gradleProperty("BUPAS_BUILDER_TAG").orElse("local").get()
val useFakeData = providers.gradleProperty("BUPAS_USE_FAKE_DATA").orElse("true").get()
val apiUrlDev = providers.gradleProperty("BUPAS_API_URL_DEV")
    .orElse("https://apisintegraciondevoci.coexito.com.co/coe-bupas/api/").get()
val apiUrlQa = providers.gradleProperty("BUPAS_API_URL_QA")
    .orElse("https://apisintegraciontestoci.coexito.com.co/coe-bupas/api/").get()
val apiUrlProd = providers.gradleProperty("BUPAS_API_URL_PROD")
    .orElse("https://apisintegracionoci.coexito.com.co/coe-bupas/api/").get()
val gitCommitBuilder = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1 }
    .orElse(1)
    .get()
val fullVersionName = "$vMajor.$vMinor.$gitCommitBuilder"

android {
    namespace = "co.com.coexito.bupas"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.com.coexito.bupas"
        minSdk = 26
        targetSdk = 34
        versionCode = gitCommitBuilder
        versionName = fullVersionName

        buildConfigField("String", "BUILDER_TAG", "\"$builderTag\"")
        buildConfigField("int", "VERSION_BUILDER", "$gitCommitBuilder")
        buildConfigField("Boolean", "USE_FAKE_DATA", useFakeData)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "BUPAS Dev")
            buildConfigField("String", "ENVIRONMENT", "\"DEV\"")
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlDev\"")
        }
        create("qa") {
            dimension = "environment"
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-test"
            resValue("string", "app_name", "BUPAS Test")
            buildConfigField("String", "ENVIRONMENT", "\"TEST\"")
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlQa\"")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "BUPAS")
            buildConfigField("String", "ENVIRONMENT", "\"PROD\"")
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlProd\"")
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
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

base {
    archivesName.set("app-bupas-v$fullVersionName-$builderTag")
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
    implementation(libs.androidx.compose.ui.text.google.fonts)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
